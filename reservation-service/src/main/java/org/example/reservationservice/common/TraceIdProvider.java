package org.example.reservationservice.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TraceIdProvider {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    public String resolve(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        return (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
    }
}