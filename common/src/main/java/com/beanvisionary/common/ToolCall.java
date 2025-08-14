package com.beanvisionary.common;

import java.util.Map;

public record ToolCall(String name, Map<String, Object> args) {}
