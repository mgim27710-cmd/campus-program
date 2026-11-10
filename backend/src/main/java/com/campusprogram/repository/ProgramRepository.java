package com.campusprogram.repository;

import com.campusprogram.domain.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// [2단계-② Repository] programs 테이블을 다루는 창구
// save, findById, findAll 같은 기본 메서드는 JpaRepository가 자동으로 만들어 준다
public interface ProgramRepository extends JpaRepository<Program, Long> {

    // 신청 시작 시각이 빠른 순서로 전체 목록 조회
    List<Program> findAllByOrderByApplyStartAtAsc();
}
