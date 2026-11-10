package com.campusprogram.exception;

// [2단계-③ 예외] 서비스 코드에서 "이 요청은 실패"라고 알릴 때 던지는 예외
// 예: throw new ApiException(ErrorCode.PROGRAM_FULL);
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
