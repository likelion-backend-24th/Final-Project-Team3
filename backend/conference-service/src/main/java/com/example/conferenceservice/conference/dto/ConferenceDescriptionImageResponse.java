package com.example.conferenceservice.conference.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소개글 삽입용 이미지 업로드 응답")
public record ConferenceDescriptionImageResponse(
        @Schema(description = "소개글 텍스트에 `![](url)` 형태로 붙여 넣을 이미지 URL") String imageUrl
) {
    public static ConferenceDescriptionImageResponse of(String storedFilename) {
        return new ConferenceDescriptionImageResponse(ConferenceImageUrls.of(storedFilename));
    }
}
