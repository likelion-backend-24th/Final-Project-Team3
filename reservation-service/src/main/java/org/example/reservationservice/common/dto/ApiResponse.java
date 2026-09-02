package org.example.reservationservice.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

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

    public static <T> ApiResponse<T> success(String message, T data, String traceId) {
        return new ApiResponse<>(true, message, data, null, null, traceId);
    }

    public static <T> ApiResponse<T> success(String message, T data, Meta meta, String traceId) {
        return new ApiResponse<>(true, message, data, meta, null, traceId);
    }

    public static <T> ApiResponse<T> success(String message, String traceId) {
        return new ApiResponse<>(true, message, null, null, null, traceId);
    }

    public static <T> ApiResponse<T> error(String code, String message, String traceId) {
        return new ApiResponse<>(false, null, null, null, new ErrorDetail(code, message), traceId);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private final String code;
        private final String message;

        public ErrorDetail(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}