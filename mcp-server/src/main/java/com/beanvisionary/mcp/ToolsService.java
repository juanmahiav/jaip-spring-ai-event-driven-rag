package com.beanvisionary.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.*;

@Service
public class ToolsService {
    private final Map<String, Map<String, Object>> orders = new HashMap<>();
    private final List<String> sanctions = new ArrayList<>();
    private final SecureRandom rnd = new SecureRandom();

    public ToolsService(ObjectMapper om) {
        orders.putAll(readJson(om, "/data/orders.json", new TypeReference<>() {}));
        sanctions.addAll(readJson(om, "/data/sanctions.json", new TypeReference<>() {}));
    }

    private String campaignId() {
        return "cmp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public Map<String, Object> launchCampaign(Map<String, Object> args) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("campaignId", campaignId());
        out.put("status", "SCHEDULED");
        if (args != null && args.containsKey("budget")) {
            out.put("budget", args.get("budget"));
        }
        return out;
    }

    public Map<String, Object> lookupOrder(String orderId) {
        return orders.getOrDefault(orderId, Map.of("error", "NOT_FOUND", "orderId", orderId));
    }

    public Map<String, Object> checkSanctions(String name) {
        boolean hit = Optional.ofNullable(name)
                .map(String::toLowerCase)
                .map(n -> sanctions.stream().anyMatch(s -> s.contains(n) || n.contains(s)))
                .orElse(false);
        return Map.of(
                "match", hit,
                "score", hit ? 0.95 : 0.02,
                "rule", hit ? "contains" : "no-hit"
        );
    }

    @SneakyThrows
    private <T> T readJson(ObjectMapper om, String cp, TypeReference<T> ref) {
        try (InputStream is = getClass().getResourceAsStream(cp)) {
            if (is == null) throw new IllegalStateException("Missing resource: " + cp);
            return om.readValue(is, ref);
        }
    }
}
