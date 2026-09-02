package com.example.conferenceservice.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final ErrorDetail error;
    private final Meta meta;
    private final String traceId;

    private ApiResponse(boolean success, T data, String message, ErrorDetail error, Meta meta, String traceId) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.error = error;
        this.meta = meta;
        this.traceId = traceId;
    }

    // ==================== 성공 팩토리 메서드 ====================
    // 반환값이 있을 때
    public static <T> ApiResponse<T> success(String message, T data, String traceId) {
        return new ApiResponse<>(true, data, message, null, null, traceId);
    }

    // 반환값 + 페이지네이션 메타가 있을 때
    public static <T> ApiResponse<T> success(String message, T data, Meta meta, String traceId) {
        return new ApiResponse<>(true, data, message, null, meta, traceId);
    }

    // 반환값이 없을 때
    public static <T> ApiResponse<T> success(String message, String traceId) {
        return new ApiResponse<>(true, null, message, null, null, traceId);
    }

    // ==================== 에러 팩토리 메서드 ====================
    public static <T> ApiResponse<T> error(String code, String message, String traceId) {
        return new ApiResponse<>(false, null, null, new ErrorDetail(code, message), null, traceId);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public ErrorDetail getError() { return error; }
    public Meta getMeta() { return meta; }
    public String getTraceId() { return traceId; }

    // ==================== 에러 상세 ====================
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private final String code;
        private final String message;

        public ErrorDetail(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}