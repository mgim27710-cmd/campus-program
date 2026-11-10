package com.campusprogram.controller;

import com.campusprogram.dto.ProgramResponse;
import com.campusprogram.service.ProgramService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// [2단계-⑥ Controller] 기능 1: 프로그램 목록 조회 / 기능 4: 신청 현황 확인
@RestController
@RequestMapping("/api/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    // GET /api/programs → 전체 목록
    @GetMapping
    public List<ProgramResponse> getPrograms() {
        return programService.getPrograms();
    }

    // GET /api/programs/1 → 1번 프로그램 상세·현황
    @GetMapping("/{programId}")
    public ProgramResponse getProgram(@PathVariable Long programId) {
        return programService.getProgram(programId);
    }
}
