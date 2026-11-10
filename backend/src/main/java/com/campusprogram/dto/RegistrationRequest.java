package com.campusprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 신청·취소 요청 본문: {"studentId": "20260001", "studentName": "김민규"}
// 어노테이션 = 값이 들어오자마자 자동으로 검사할 규칙
public record RegistrationRequest(

        @NotBlank(message = "학번을 입력해 주세요.")
        @Pattern(regexp = "\\d{6,10}", message = "학번은 숫자 7자리로 입력해 주세요.")
        String studentId,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 20, message = "이름은 20자 이하로 입력해 주세요.")
        String studentName
) {
}
