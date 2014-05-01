package com.chatwala.android.networking.requests;

/**
 * Created by samirahman on 4/4/14.
 */
public class GetMessageReadUrlResponse {
    private int code;
    private String messageId;
    private String readUrl;

    public GetMessageReadUrlResponse(int code, String messageId, String readUrl) {
        this.code = code;
        this.messageId = messageId;
        this.readUrl = readUrl;
    }

    public int getCode() {
        return code;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getReadUrl() {
        return readUrl;
    }
}
