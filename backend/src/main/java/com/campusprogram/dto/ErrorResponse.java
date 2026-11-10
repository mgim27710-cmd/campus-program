package com.campusprogram.dto;

// 실패 응답 모양: {"code": "PROGRAM_FULL", "message": "정원이 모두 찼습니다."}
public record ErrorResponse(String code, String message) {
}
