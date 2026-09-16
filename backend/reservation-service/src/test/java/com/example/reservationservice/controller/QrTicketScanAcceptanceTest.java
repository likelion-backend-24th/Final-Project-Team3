package com.example.reservationservice.controller;

import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class QrTicketScanAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QrTicketRepository qrTicketRepository;

    @AfterEach
    void tearDown() {
        qrTicketRepository.deleteAll();
    }

    @Test
    @DisplayName("유효한 QR 티켓은 정상적으로 스캔되어 입장 처리된다")
    void 정상_스캔() throws Exception {
        QrTicket ticket = QrTicket.builder()
                .reservationId(UUID.randomUUID())
                .code("VALID-CODE-1")
                .build();
        qrTicketRepository.save(ticket);

        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "VALID-CODE-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.used").value(true));
    }

    @Test
    @DisplayName("존재하지 않는 코드는 404로 거부된다")
    void 존재하지않는_코드_404() throws Exception {
        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "NOT-EXIST"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("이미 사용된 코드는 409로 거부된다")
    void 이미사용된_코드_409() throws Exception {
        QrTicket ticket = QrTicket.builder()
                .reservationId(UUID.randomUUID())
                .code("USED-CODE")
                .build();
        ticket.scan();
        qrTicketRepository.save(ticket);

        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "USED-CODE"))
                .andExpect(status().isConflict());
    }
}