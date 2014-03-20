package com.chatwala.android.networking;

/**
 * Created by samirahman on 3/20/14.
 */
public class HttpResponseResult<T> {
    private T responseData;
    private int responseCode;

    public HttpResponseResult(T responseData, int responseCode) {
        this.responseData = responseData;
        this.responseCode = responseCode;
    }

    public T getResponseData() {
        return responseData;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
