package com.campusprogram.dto;

import com.campusprogram.domain.Registration;

import java.time.LocalDateTime;

// 내 신청 내역 1건
public record MyRegistrationResponse(
        Long programId,
        String programTitle,
        LocalDateTime registeredAt
) {

    public static MyRegistrationResponse from(Registration registration) {
        return new MyRegistrationResponse(
                registration.getProgram().getId(),
                registration.getProgram().getTitle(),
                registration.getCreatedAt()
        );
    }
}
