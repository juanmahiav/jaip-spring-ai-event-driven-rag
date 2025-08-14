package com.beanvisionary.common;

import java.util.Map;

public record ToolResult(String name, Map<String, Object> result, String error) {}
