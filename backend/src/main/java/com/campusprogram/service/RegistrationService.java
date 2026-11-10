package com.campusprogram.service;

import com.campusprogram.domain.Program;
import com.campusprogram.domain.Registration;
import com.campusprogram.dto.MyRegistrationResponse;
import com.campusprogram.dto.RegistrationRequest;
import com.campusprogram.dto.RegistrationResponse;
import com.campusprogram.exception.ApiException;
import com.campusprogram.exception.ErrorCode;
import com.campusprogram.repository.ProgramRepository;
import com.campusprogram.repository.RegistrationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// [2단계-⑤ Service] 기능 2: 참가 신청 / 기능 3: 신청 취소
//
// 주의: apply, cancel 메서드에는 @Transactional을 붙이지 않았다.
//   repository.save() 자체가 짧은 트랜잭션으로 바로 커밋되기 때문에,
//   저장 실패를 이 메서드 안의 try-catch에서 바로 잡고 Redis 값을 되돌릴 수 있다.
@Service
public class RegistrationService {

    private final ProgramRepository programRepository;
    private final RegistrationRepository registrationRepository;
    private final SeatCounter seatCounter;

    public RegistrationService(ProgramRepository programRepository,
                               RegistrationRepository registrationRepository,
                               SeatCounter seatCounter) {
        this.programRepository = programRepository;
        this.registrationRepository = registrationRepository;
        this.seatCounter = seatCounter;
    }

    // ===== 기능 2: 참가 신청 =====
    public RegistrationResponse apply(Long programId, RegistrationRequest request) {
        String studentId = request.studentId();
        String studentName = request.studentName().trim();

        // 1단계: 프로그램이 존재하는지 확인
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROGRAM_NOT_FOUND));

        // 2단계: 신청 기간인지 확인
        LocalDateTime now = LocalDateTime.now();
        if (program.isBeforeApplyPeriod(now)) {
            throw new ApiException(ErrorCode.APPLY_NOT_STARTED);
        }
        if (program.isAfterApplyPeriod(now)) {
            throw new ApiException(ErrorCode.APPLY_CLOSED);
        }

        // 3단계: Redis에 이 프로그램의 정원 정보가 준비되어 있는지 확인
        seatCounter.ensureLoaded(program);

        // 4단계: 중복 신청 확인 (신청자 집합에 학번 넣기 시도)
        boolean isNewApplicant = seatCounter.addApplicant(programId, studentId);
        if (!isNewApplicant) {
            throw new ApiException(ErrorCode.ALREADY_REGISTERED);
        }

        // 5단계: 자리 차지 (잔여석 1 감소)
        long remainingAfter = seatCounter.takeSeat(programId);
        if (remainingAfter < 0) {
            // 자리가 없었음 → 방금 한 일을 모두 되돌리고 실패 응답
            seatCounter.returnSeat(programId);
            seatCounter.removeApplicant(programId, studentId);
            throw new ApiException(ErrorCode.PROGRAM_FULL);
        }

        // 6단계: MySQL에 신청 기록 저장 (원본 기록)
        Registration saved;
        try {
            saved = registrationRepository.save(Registration.create(program, studentId, studentName));
        } catch (DataIntegrityViolationException e) {
            // DB에는 이미 같은 학번의 신청이 있었음 → 자리만 돌려주고 중복 신청으로 응답
            seatCounter.returnSeat(programId);
            throw new ApiException(ErrorCode.ALREADY_REGISTERED);
        } catch (RuntimeException e) {
            // 그 밖의 저장 실패 → Redis 값을 원래대로 되돌린 뒤 예외를 그대로 넘긴다
            seatCounter.returnSeat(programId);
            seatCounter.removeApplicant(programId, studentId);
            throw e;
        }

        // 7단계: 성공 응답
        return RegistrationResponse.of(saved, program.getTitle(), (int) remainingAfter);
    }

    // ===== 기능 3: 신청 취소 =====
    public void cancel(Long programId, RegistrationRequest request) {
        String studentId = request.studentId();
        String studentName = request.studentName().trim();

        // 1단계: 프로그램이 존재하는지 확인
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROGRAM_NOT_FOUND));

        // 2단계: 이 학번의 신청 기록 찾기
        Registration registration = registrationRepository.findByProgramIdAndStudentId(programId, studentId)
                .orElseThrow(() -> new ApiException(ErrorCode.REGISTRATION_NOT_FOUND));

        // 3단계: 이름이 맞는지 확인 (다른 사람이 학번만 알고 취소하는 것 방지)
        if (!registration.hasSameName(studentName)) {
            throw new ApiException(ErrorCode.NAME_MISMATCH);
        }

        // 4단계: Redis 정보를 먼저 준비해 둔다
        //        (DB에서 지운 뒤에 Redis를 채우면 잔여석이 두 번 늘어날 수 있기 때문)
        seatCounter.ensureLoaded(program);

        // 5단계: MySQL에서 신청 기록 삭제
        registrationRepository.deleteById(registration.getId());

        // 6단계: Redis에서 학번을 지우고, 실제로 지워졌을 때만 자리를 돌려준다
        //        (같은 취소 요청이 두 번 와도 자리가 두 번 늘어나지 않게)
        boolean removed = seatCounter.removeApplicant(programId, studentId);
        if (removed) {
            seatCounter.returnSeat(programId);
        }
    }

    // ===== 내 신청 내역 조회 (화면에서 "신청함" 표시용) =====
    @Transactional(readOnly = true)
    public List<MyRegistrationResponse> getMyRegistrations(String studentId) {
        // 1단계: 학번 형식 검사
        if (studentId == null || !studentId.matches("\\d{8,10}")) {
            throw new ApiException(ErrorCode.INVALID_STUDENT_ID);
        }

        // 2단계: DB에서 조회 후 응답 모양으로 바꾸기
        List<Registration> registrations = registrationRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId);
        List<MyRegistrationResponse> result = new ArrayList<>();
        for (Registration registration : registrations) {
            result.add(MyRegistrationResponse.from(registration));
        }
        return result;
    }
}
