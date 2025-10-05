package com.beanvisionary.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents a tool call with its arguments and optional request identifier.
 * 
 * @param name The name of the tool to be called
 * @param args The arguments to pass to the tool
 * @param requestId Optional identifier for tracking the request. May be null when 
 *                  the tool call is created independently of a specific request context,
 *                  such as during AI model inference or internal tool suggestions.
 */
public record ToolCall(
        @JsonProperty("name") String name,
        @JsonProperty("args") Map<String, Object> args,
        @JsonProperty("requestId") String requestId
) {

    /**
     * Convenience constructor for creating a ToolCall without a request ID.
     * The requestId will be set to null, which is acceptable for tool calls
     * that are not tied to a specific request context.
     * 
     * @param name The name of the tool to be called
     * @param args The arguments to pass to the tool
     */
    public ToolCall(String name, Map<String, Object> args) {
        this(name, args, null);
    }
}