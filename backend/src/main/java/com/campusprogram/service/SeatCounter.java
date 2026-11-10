package com.campusprogram.service;

import com.campusprogram.domain.Program;
import com.campusprogram.domain.Registration;
import com.campusprogram.repository.RegistrationRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

// [2단계-④ Redis] 선착순 정원을 Redis로 관리하는 클래스
//
// 왜 Redis인가?
//   DB에서 "현재 인원 조회 → 정원과 비교 → 저장" 순서로 처리하면
//   동시에 들어온 요청들이 같은 인원 값을 읽어서 정원을 넘겨 신청될 수 있다.
//   Redis의 DECR(1 빼기), SADD(집합에 추가)는 한 번에 하나씩 처리되는(원자적) 명령이라
//   100명이 동시에 와도 정확히 정원만큼만 통과한다.
//
// 사용하는 키
//   program:{id}:remaining   → 남은 자리 수 (문자열 숫자)
//   program:{id}:applicants  → 신청한 학번 모음 (Set)
@Component
public class SeatCounter {

    private final StringRedisTemplate redisTemplate;
    private final RegistrationRepository registrationRepository;

    public SeatCounter(StringRedisTemplate redisTemplate, RegistrationRepository registrationRepository) {
        this.redisTemplate = redisTemplate;
        this.registrationRepository = registrationRepository;
    }

    private String remainingKey(Long programId) {
        return "program:" + programId + ":remaining";
    }

    private String applicantsKey(Long programId) {
        return "program:" + programId + ":applicants";
    }

    // Redis에 이 프로그램의 키가 없으면 MySQL 기록을 보고 다시 채운다
    // (서버 첫 실행, Redis 데이터 초기화 후 등)
    public void ensureLoaded(Program program) {
        Long programId = program.getId();

        // 1단계: 잔여석 키가 이미 있으면 할 일 없음
        Boolean exists = redisTemplate.hasKey(remainingKey(programId));
        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        // 2단계: MySQL에서 신청 기록을 읽어 신청자 집합을 채운다
        List<Registration> registrations = registrationRepository.findAllByProgramId(programId);
        for (Registration registration : registrations) {
            redisTemplate.opsForSet().add(applicantsKey(programId), registration.getStudentId());
        }

        // 3단계: 잔여석 = 정원 - 신청 기록 수
        // setIfAbsent: 다른 요청이 먼저 값을 넣었다면 덮어쓰지 않는다
        int remaining = program.getCapacity() - registrations.size();
        redisTemplate.opsForValue().setIfAbsent(remainingKey(programId), String.valueOf(remaining));
    }

    // 신청자 집합에 학번 추가. 처음 추가되면 true, 이미 있으면(중복 신청) false
    public boolean addApplicant(Long programId, String studentId) {
        Long addedCount = redisTemplate.opsForSet().add(applicantsKey(programId), studentId);
        return addedCount != null && addedCount == 1L;
    }

    // 신청자 집합에서 학번 제거. 실제로 지워졌으면 true
    public boolean removeApplicant(Long programId, String studentId) {
        Long removedCount = redisTemplate.opsForSet().remove(applicantsKey(programId), studentId);
        return removedCount != null && removedCount == 1L;
    }

    // 자리 하나 차지: 잔여석을 1 줄이고, 줄어든 뒤의 값을 돌려준다
    // 돌려받은 값이 0보다 작으면 자리가 없었다는 뜻
    public long takeSeat(Long programId) {
        Long result = redisTemplate.opsForValue().decrement(remainingKey(programId));
        if (result == null) {
            return -1;
        }
        return result;
    }

    // 자리 하나 돌려주기: 잔여석을 1 늘린다 (취소, 실패 복구 시)
    public void returnSeat(Long programId) {
        redisTemplate.opsForValue().increment(remainingKey(programId));
    }

    // 현재 잔여석 조회 (화면 표시용, 0 미만이면 0으로 보여준다)
    public int getRemainingSeats(Program program) {
        ensureLoaded(program);

        String value = redisTemplate.opsForValue().get(remainingKey(program.getId()));
        if (value == null) {
            return 0;
        }

        int remaining = Integer.parseInt(value);
        return Math.max(remaining, 0);
    }
}
