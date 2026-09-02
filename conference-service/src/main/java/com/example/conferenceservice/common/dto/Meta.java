package com.example.conferenceservice.common.dto;

import com.example.conferenceservice.common.dto.PageMeta;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Meta {
    private PageMeta pagination;
}