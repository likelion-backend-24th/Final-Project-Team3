package com.example.conferenceservice.conference.dto;

import com.example.conferenceservice.conference.entity.Conference;

// FileStorageService가 저장한 파일명을 공개 조회 엔드포인트(ConferenceController#getImage) URL로 바꾼다.
final class ConferenceImageUrls {

    private static final String IMAGE_PATH_PREFIX = "/api/conferences/images/";

    private ConferenceImageUrls() {
    }

    static String thumbnailOf(Conference conference) {
        return toUrl(conference.getThumbnailImageName());
    }

    static String detailOf(Conference conference) {
        return toUrl(conference.getDetailImageName());
    }

    static String of(String storedFilename) {
        return toUrl(storedFilename);
    }

    private static String toUrl(String storedFilename) {
        return storedFilename == null ? null : IMAGE_PATH_PREFIX + storedFilename;
    }
}
