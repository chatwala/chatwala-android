package com.chatwala.android.networking.requests;

/**
 * Created by samirahman on 4/4/14.
 */
public class GetMessageReadUrlResponse {

    String messageId;
    String readUrl;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getReadUrl() {
        return readUrl;
    }

    public void setReadUrl(String readUrl) {
        this.readUrl = readUrl;
    }
}
