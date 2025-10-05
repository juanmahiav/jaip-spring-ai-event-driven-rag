package com.beanvisionary.common;

public class AiResponse {
    private String requestId;
    private String response;

    public AiResponse() {
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}