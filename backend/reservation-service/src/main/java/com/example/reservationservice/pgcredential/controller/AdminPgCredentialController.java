package com.example.reservationservice.pgcredential.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.pgcredential.dto.PgCredentialRequest;
import com.example.reservationservice.pgcredential.dto.PgCredentialResponse;
import com.example.reservationservice.pgcredential.service.PgCredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "시스템 설정", description = "전체관리자 전용 시스템 설정 API")
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminPgCredentialController {

    private final PgCredentialService pgCredentialService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "PG 연동 키 등록·수정", description = "전체관리자(ADMIN)가 PortOne 연동 키를 등록하거나 갱신한다. provider별 1건만 저장되며 "
            + "apiSecret·webhookSecret은 AES로 암호화되어 저장되고, 응답에는 뒤 4자리만 보이는 마스킹 값이 나간다. ADMIN이 아니면 403")
    @PatchMapping("/pg-key")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PgCredentialResponse>> registerPgKey(
            @Valid @RequestBody PgCredentialRequest request, HttpServletRequest httpRequest) {
        PgCredentialResponse response = pgCredentialService.registerOrUpdate(request);
        return ResponseEntity.ok(
                ApiResponse.success("PG 연동 키 저장 완료", response, traceIdProvider.resolve(httpRequest)));
    }
}
