package com.campusprogram.dto;

import com.campusprogram.domain.Program;

import java.time.LocalDateTime;

// 프로그램 목록·상세 응답 (신청 현황 포함)
public record ProgramResponse(
        Long id,
        String title,
        String category,
        String categoryLabel,
        String description,
        String location,
        LocalDateTime eventAt,
        LocalDateTime applyStartAt,
        LocalDateTime applyEndAt,
        int capacity,
        int appliedCount,
        int remainingSeats,
        String status,
        String statusLabel
) {

    // program: DB에서 꺼낸 프로그램, remainingSeats: Redis에서 읽은 잔여석, now: 현재 시각
    public static ProgramResponse of(Program program, int remainingSeats, LocalDateTime now) {

        // 1단계: 현재 상태 결정 (신청 예정 / 기간 종료 / 정원 마감 / 신청 가능)
        String status;
        String statusLabel;
        if (program.isBeforeApplyPeriod(now)) {
            status = "UPCOMING";
            statusLabel = "신청 예정";
        } else if (program.isAfterApplyPeriod(now)) {
            status = "CLOSED";
            statusLabel = "기간 종료";
        } else if (remainingSeats <= 0) {
            status = "FULL";
            statusLabel = "정원 마감";
        } else {
            status = "OPEN";
            statusLabel = "신청 가능";
        }

        // 2단계: 신청 인원 = 정원 - 잔여석
        int appliedCount = program.getCapacity() - remainingSeats;

        // 3단계: 응답 객체 만들기
        return new ProgramResponse(
                program.getId(),
                program.getTitle(),
                program.getCategory().name(),
                program.getCategory().getLabel(),
                program.getDescription(),
                program.getLocation(),
                program.getEventAt(),
                program.getApplyStartAt(),
                program.getApplyEndAt(),
                program.getCapacity(),
                appliedCount,
                remainingSeats,
                status,
                statusLabel
        );
    }
}
