package com.chatwala.android.http.server20;

/**
 * Created by samirahman on 3/14/14.
 */
public class ChatwalaResponse<T> {

    private T responseData;
    private int responseCode;
    private String responseMessage;

    public T getResponseData() {
        return responseData;
    }

    public void setResponseData(T responseData) {
        this.responseData = responseData;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }
}
