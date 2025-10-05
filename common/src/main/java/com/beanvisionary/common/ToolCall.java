package com.beanvisionary.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ToolCall(
        @JsonProperty("name") String name,
        @JsonProperty("args") Map<String, Object> args,
        @JsonProperty("requestId") String requestId
) {

    public ToolCall(String name, Map<String, Object> args) {
        this(name, args, null);
    }
}