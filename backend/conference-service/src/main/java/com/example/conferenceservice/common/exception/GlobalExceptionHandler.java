package com.example.conferenceservice.common.exception;

import com.example.conferenceservice.auth.exception.AuthErrorCode;
import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final TraceIdProvider traceIdProvider;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        return build(e.getErrorCode(), e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse(CommonErrorCode.INVALID_REQUEST.getMessage());
        return build(CommonErrorCode.INVALID_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = "%s: 잘못된 형식의 값입니다.".formatted(e.getName());
        return build(CommonErrorCode.INVALID_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return build(CommonErrorCode.INVALID_REQUEST, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return build(AuthErrorCode.ACCESS_DENIED, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), e);
        return build(CommonErrorCode.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode, HttpServletRequest request) {
        return build(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage(), request);
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode, String message, HttpServletRequest request) {
        return build(errorCode.getHttpStatus(), errorCode.getCode(), message, request);
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        ApiResponse<Void> body = ApiResponse.error(code, message, traceIdProvider.resolve(request));
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (status == HttpStatus.UNAUTHORIZED) {
            builder.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return builder.body(body);
    }
}