package com.chatwala.android.networking;

/**
 * Created by samirahman on 3/20/14.
 */
public class HttpResponseResult<T> {

    private T responseData;

    public T getResponseData() {
        return responseData;
    }

    public void setResponseData(T response) {
        this.responseData = response;
    }
}
