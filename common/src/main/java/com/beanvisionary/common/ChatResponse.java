package com.beanvisionary.common;

import java.time.Instant;
import java.util.List;

public record ChatResponse(
        String requestId,
        String userId,
        String sessionId,
        String answer,
        List<ToolCall> toolCalls,
        List<String> citations,
        Instant ts
) {}
