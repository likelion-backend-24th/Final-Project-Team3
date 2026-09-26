package com.example.conferenceservice.conference.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 소개글 본문에 프론트(DescriptionEditor)가 심어두는 `![alt](url)` 마크다운 이미지 문법을 해석한다.
// 프론트 DescriptionText.jsx의 정규식과 반드시 같은 패턴을 유지해야 한다.
final class DescriptionMarkdownImages {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)]\\((\\S+)\\)");
    private static final String IMAGE_URL_PREFIX = "/api/conferences/images/";

    private DescriptionMarkdownImages() {
    }

    static int countImages(String description) {
        if (description == null) {
            return 0;
        }
        int count = 0;
        Matcher matcher = IMAGE_PATTERN.matcher(description);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    // 우리 업로드 엔드포인트가 만든 URL만 신뢰한다 - 소개글 텍스트에 외부 URL이 손으로 적혀 있어도
    // 서버가 그 주소로 직접 요청을 보내지 않도록(SSRF 방지) 분석 대상에서 제외한다.
    static String firstStoredImageFilename(String description) {
        if (description == null) {
            return null;
        }
        Matcher matcher = IMAGE_PATTERN.matcher(description);
        if (matcher.find()) {
            String url = matcher.group(2);
            if (url.startsWith(IMAGE_URL_PREFIX)) {
                return url.substring(IMAGE_URL_PREFIX.length());
            }
        }
        return null;
    }
}
