package com.beanvisionary.ai.service;

import com.beanvisionary.common.ToolCall;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ToolParsing {
    private static final ObjectMapper M = new ObjectMapper();
    static ToolCall maybeParseTool(String content) {
        try {
            content = content.trim();
            if (!content.startsWith("{")) return null;
            var n = M.readTree(content);
            if (!n.has("tool")) return null;
            String name = n.get("tool").asText();
            Map<String,Object> args = n.has("args") ? M.convertValue(n.get("args"), new TypeReference<>(){}) : Map.of();
            return new ToolCall(name, args);
        } catch (Exception e) { return null; }
    }
    
    static List<ToolCall> extractToolCalls(String content) {
        List<ToolCall> toolCalls = new ArrayList<>();
        ToolCall toolCall = maybeParseTool(content);
        if (toolCall != null) {
            toolCalls.add(toolCall);
        }
        return toolCalls;
    }
}