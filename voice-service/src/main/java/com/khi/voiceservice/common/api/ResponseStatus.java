package com.khi.voiceservice.common.api;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ResponseStatus {

    SUCCESS("success"),
    FAILURE("failure"),
    ERROR("error");

    @JsonValue
    private final String code;
}