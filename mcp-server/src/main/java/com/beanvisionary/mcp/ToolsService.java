package com.beanvisionary.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        System.out.println("=== SANCTIONS CHECK DEBUG ===");
        System.out.println("Input name: '" + name + "'");
        System.out.println("Sanctions list: " + sanctions);
        
        boolean hit = Optional.ofNullable(name)
                .map(String::toLowerCase)
                .map(n -> {
                    System.out.println("Lowercase name: '" + n + "'");
                    boolean foundMatch = sanctions.stream().anyMatch(s -> {
                        boolean contains = s.contains(n) || n.contains(s);
                        System.out.println("Checking '" + s + "' against '" + n + "' -> " + contains);
                        return contains;
                    });
                    System.out.println("Overall match result: " + foundMatch);
                    return foundMatch;
                })
                .orElse(false);
        
        Map<String, Object> result = Map.of(
                "match", hit,
                "score", hit ? 0.95 : 0.02,
                "rule", hit ? "contains" : "no-hit"
        );
        System.out.println("Final result: " + result);
        System.out.println("=== END SANCTIONS CHECK DEBUG ===");
        
        return result;
    }

    private <T> T readJson(ObjectMapper om, String cp, TypeReference<T> ref) {
        try (InputStream is = getClass().getResourceAsStream(cp)) {
            if (is == null) throw new IllegalStateException("Missing resource: " + cp);
            return om.readValue(is, ref);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON resource: " + cp, e);
        }
    }
}