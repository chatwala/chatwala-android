package com.chatwala.android.http.server20;

/**
 * Created by samirahman on 3/17/14.
 */
public class ChatwalaMessageStartInfo {

    String shareUrl;
    String shortUrl;
    String messageId;

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl=shortUrl;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
