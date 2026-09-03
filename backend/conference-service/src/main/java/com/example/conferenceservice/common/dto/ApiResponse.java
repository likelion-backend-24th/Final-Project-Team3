package com.example.conferenceservice.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "message", "data", "meta", "error", "traceId"})
public class ApiResponse<T> {

    // Getters
    private final boolean success;
    private final String message;
    private final T data;
    private final Meta meta;
    private final ErrorDetail error;
    private final String traceId;

    private ApiResponse(boolean success, String message, T data, Meta meta, ErrorDetail error, String traceId) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.meta = meta;
        this.error = error;
        this.traceId = traceId;
    }

    // ==================== 성공 팩토리 메서드 ====================
    // 반환값이 있을 때
    public static <T> ApiResponse<T> success(String message, T data, String traceId) {
        return new ApiResponse<>(true, message, data, null, null, traceId);
    }

    // 반환값 + 페이지네이션 메타가 있을 때
    public static <T> ApiResponse<T> success(String message, T data, Meta meta, String traceId) {
        return new ApiResponse<>(true, message, data, meta, null, traceId);
    }

    // 반환값이 없을 때
    public static <T> ApiResponse<T> success(String message, String traceId) {
        return new ApiResponse<>(true, message, null, null, null, traceId);
    }

    // ==================== 에러 팩토리 메서드 ====================
    public static <T> ApiResponse<T> error(String code, String message, String traceId) {
        return new ApiResponse<>(false, null, null, null, new ErrorDetail(code, message), traceId);
    }

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