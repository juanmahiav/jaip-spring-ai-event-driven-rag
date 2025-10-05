package com.beanvisionary.common;

public class AiRequest {
    private String requestId;
    private String query;

    public AiRequest() {
    }

    public AiRequest(String requestId, String query) {
        this.requestId = requestId;
        this.query = query;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}