package com.campusprogram.exception;

import com.campusprogram.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// [2단계-③ 예외] 어느 Controller에서 예외가 나든 여기서 잡아서
// {"code": "...", "message": "..."} 형태의 같은 모양으로 응답한다
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1) 우리가 직접 던진 예외 (정원 초과, 중복 신청 등)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse body = new ErrorResponse(errorCode.name(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    // 2) @Valid 검사 실패 (학번 형식 오류, 이름 빈칸 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = ErrorCode.INVALID_INPUT.getMessage();

        // 첫 번째로 틀린 항목의 안내 문구를 꺼낸다
        FieldError firstError = e.getBindingResult().getFieldError();
        if (firstError != null && firstError.getDefaultMessage() != null) {
            message = firstError.getDefaultMessage();
        }

        ErrorResponse body = new ErrorResponse(ErrorCode.INVALID_INPUT.name(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 3) 요청 본문이 JSON 형식이 아니거나 비어 있음
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        ErrorResponse body = new ErrorResponse(ErrorCode.INVALID_INPUT.name(), "요청 형식이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 4) 필수 쿼리 파라미터 누락 (예: /api/registrations 에 studentId 없음)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        ErrorResponse body = new ErrorResponse(ErrorCode.INVALID_INPUT.name(), e.getParameterName() + " 값이 필요합니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 5) 주소의 숫자 자리에 문자가 들어옴 (예: /api/programs/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        ErrorResponse body = new ErrorResponse(ErrorCode.INVALID_INPUT.name(), "주소 형식이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 6) 존재하지 않는 API 주소
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        ErrorResponse body = new ErrorResponse("NOT_FOUND", "존재하지 않는 주소입니다.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 7) 그 밖의 예상하지 못한 오류 → 로그를 남기고 500 응답
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("예상하지 못한 오류", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        ErrorResponse body = new ErrorResponse(errorCode.name(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }
}
