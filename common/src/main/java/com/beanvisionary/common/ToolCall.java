package com.beanvisionary.common;

import java.util.Map;

public class ToolCall {
    private String name;
    private Map<String, Object> args;
    private String requestId;

    public ToolCall() {
    }

    public ToolCall(String name, Map<String, Object> args) {
        this.name = name;
        this.args = args;
    }

    public ToolCall(String name, Map<String, Object> args, String requestId) {
        this.name = name;
        this.args = args;
        this.requestId = requestId;
    }
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }
}