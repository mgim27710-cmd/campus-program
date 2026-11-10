package com.campusprogram.dto;

import com.campusprogram.domain.Registration;

import java.time.LocalDateTime;

// 신청 성공 응답
public record RegistrationResponse(
        Long registrationId,
        Long programId,
        String programTitle,
        String studentId,
        String studentName,
        int remainingSeats,
        LocalDateTime registeredAt
) {

    public static RegistrationResponse of(Registration registration, String programTitle, int remainingSeats) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getProgram().getId(),
                programTitle,
                registration.getStudentId(),
                registration.getStudentName(),
                remainingSeats,
                registration.getCreatedAt()
        );
    }
}
