package com.example.reservationservice.pgcredential.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.pgcredential.dto.PgCredentialRequest;
import com.example.reservationservice.pgcredential.dto.PgCredentialResponse;
import com.example.reservationservice.pgcredential.service.PgCredentialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminPgCredentialController {

    private final PgCredentialService pgCredentialService;
    private final TraceIdProvider traceIdProvider;

    @PatchMapping("/pg-key")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PgCredentialResponse>> registerPgKey(
            @Valid @RequestBody PgCredentialRequest request, HttpServletRequest httpRequest) {
        PgCredentialResponse response = pgCredentialService.registerOrUpdate(request);
        return ResponseEntity.ok(
                ApiResponse.success("PG 연동 키 저장 완료", response, traceIdProvider.resolve(httpRequest)));
    }
}
