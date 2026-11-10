package com.campusprogram.service;

import com.campusprogram.domain.Program;
import com.campusprogram.dto.ProgramResponse;
import com.campusprogram.exception.ApiException;
import com.campusprogram.exception.ErrorCode;
import com.campusprogram.repository.ProgramRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// [2단계-⑤ Service] 기능 1: 프로그램 목록 조회 / 기능 4: 신청 현황 확인
@Service
@Transactional(readOnly = true)
public class ProgramService {

    private final ProgramRepository programRepository;
    private final SeatCounter seatCounter;

    public ProgramService(ProgramRepository programRepository, SeatCounter seatCounter) {
        this.programRepository = programRepository;
        this.seatCounter = seatCounter;
    }

    // 전체 프로그램 목록 + 각 프로그램의 신청 현황
    public List<ProgramResponse> getPrograms() {
        LocalDateTime now = LocalDateTime.now();

        // 1단계: DB에서 프로그램 목록 가져오기
        List<Program> programs = programRepository.findAllByOrderByApplyStartAtAsc();

        // 2단계: 각 프로그램의 잔여석은 Redis에서 읽어서 응답 만들기
        //        (신청이 몰리는 시간에 DB 대신 Redis를 읽어 부하를 줄인다)
        List<ProgramResponse> result = new ArrayList<>();
        for (Program program : programs) {
            int remainingSeats = seatCounter.getRemainingSeats(program);
            result.add(ProgramResponse.of(program, remainingSeats, now));
        }
        return result;
    }

    // 프로그램 1개의 상세 정보 + 신청 현황
    public ProgramResponse getProgram(Long programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROGRAM_NOT_FOUND));

        int remainingSeats = seatCounter.getRemainingSeats(program);
        return ProgramResponse.of(program, remainingSeats, LocalDateTime.now());
    }
}
