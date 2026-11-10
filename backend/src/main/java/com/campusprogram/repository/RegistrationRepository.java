package com.campusprogram.repository;

import com.campusprogram.domain.Registration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// [2단계-② Repository] registrations 테이블을 다루는 창구
// 메서드 이름만 규칙대로 지으면 스프링이 SQL을 자동으로 만들어 준다
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    // 특정 프로그램에 특정 학번이 신청한 기록 1건 찾기 (취소할 때 사용)
    Optional<Registration> findByProgramIdAndStudentId(Long programId, String studentId);

    // 특정 프로그램의 신청 기록 전체 (Redis를 다시 채울 때 사용)
    List<Registration> findAllByProgramId(Long programId);

    // 특정 학번의 신청 내역 (프로그램 정보도 함께 가져오도록 EntityGraph 지정)
    @EntityGraph(attributePaths = "program")
    List<Registration> findAllByStudentIdOrderByCreatedAtDesc(String studentId);
}
