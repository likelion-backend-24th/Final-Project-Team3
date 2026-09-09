package com.example.conferenceservice.notice.dto;

import jakarta.validation.constraints.NotBlank;

public record NoticeRequest(
        @NotBlank(message = "공지 제목은 필수입니다.")
        String title,

        @NotBlank(message = "공지 내용은 필수입니다.")
        String content
) {
}
