package com.beanvisionary.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.*;

@Service
public class ToolsService {
    private static final Logger logger = LoggerFactory.getLogger(ToolsService.class);
    
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
        logger.debug("=== SANCTIONS CHECK DEBUG ===");
        logger.debug("Input name: '{}'", name);
        logger.debug("Sanctions list: {}", sanctions);
        
        boolean hit = Optional.ofNullable(name)
                .map(String::toLowerCase)
                .map(n -> {
                    logger.debug("Lowercase name: '{}'", n);
                    boolean foundMatch = sanctions.stream().anyMatch(s -> {
                        boolean contains = s.contains(n) || n.contains(s);
                        logger.debug("Checking '{}' against '{}' -> {}", s, n, contains);
                        return contains;
                    });
                    logger.debug("Overall match result: {}", foundMatch);
                    return foundMatch;
                })
                .orElse(false);
        
        Map<String, Object> result = Map.of(
                "match", hit,
                "score", hit ? 0.95 : 0.02,
                "rule", hit ? "contains" : "no-hit"
        );
        logger.debug("Final result: {}", result);
        logger.debug("=== END SANCTIONS CHECK DEBUG ===");
        
        return result;
    }

    private <T> T readJson(ObjectMapper om, String cp, TypeReference<T> ref) {
        try (InputStream is = getClass().getResourceAsStream(cp)) {
            if (is == null) throw new IllegalStateException("Missing resource: " + cp);
            return om.readValue(is, ref);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON from resource: " + cp, e);
        } catch (java.io.IOException e) {
            throw new RuntimeException("I/O error reading JSON resource: " + cp, e);
        }
    }
}