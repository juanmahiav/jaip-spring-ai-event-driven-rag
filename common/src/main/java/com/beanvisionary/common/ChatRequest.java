package com.beanvisionary.common;

import java.time.Instant;
import java.util.Map;

public record ChatRequest(
        String requestId,
        String userId,
        String sessionId,
        String query,
        Map<String, Object> metadata,
        Instant ts
) {}
