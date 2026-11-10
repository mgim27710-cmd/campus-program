package com.campusprogram.exception;

import org.springframework.http.HttpStatus;

// [2단계-③ 예외] 실패 상황을 한곳에 모아 이름을 붙여 둔 목록
// 각 항목 = (HTTP 상태코드, 화면에 보여줄 안내 문구)
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    INVALID_STUDENT_ID(HttpStatus.BAD_REQUEST, "학번은 숫자 7자리로 입력해 주세요."),
    PROGRAM_NOT_FOUND(HttpStatus.NOT_FOUND, "프로그램을 찾을 수 없습니다."),
    APPLY_NOT_STARTED(HttpStatus.CONFLICT, "아직 신청 기간이 아닙니다. 신청 시작 시각 이후에 다시 시도해 주세요."),
    APPLY_CLOSED(HttpStatus.CONFLICT, "신청 기간이 끝난 프로그램입니다."),
    ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 신청한 프로그램입니다."),
    PROGRAM_FULL(HttpStatus.CONFLICT, "정원이 모두 찼습니다."),
    REGISTRATION_NOT_FOUND(HttpStatus.NOT_FOUND, "이 학번으로 신청한 내역이 없습니다."),
    NAME_MISMATCH(HttpStatus.FORBIDDEN, "신청할 때 입력한 이름과 일치하지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
