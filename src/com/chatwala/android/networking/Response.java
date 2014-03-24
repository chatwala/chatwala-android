package com.chatwala.android.networking;

import java.util.List;
import java.util.Map;

public class Response<T> {
    private Map<String, List<String>> headers;
    private Integer code;
    private String status;
    private T data;

    public Response(Map<String, List<String>> headers, Integer code, String status, T data) {
        this.headers = headers;
        this.code = code;
        this.status = status;
        this.data = data;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Integer getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }
}
