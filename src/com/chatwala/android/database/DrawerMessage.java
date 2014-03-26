package com.chatwala.android.database;

/**
 * Created by Eliezer on 3/25/2014.
 */
public class DrawerMessage {
    private String messageId;
    private String readUrl;
    private String senderId;
    private long timestamp;
    private String thumbnailUrl;
    private ChatwalaMessage.MessageState messageState;

    public DrawerMessage(String messageId, String readUrl, String senderId, long timestamp, String thumbnailUrl, ChatwalaMessage.MessageState messageState) {
        this.messageId = messageId;
        this.readUrl = readUrl;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.thumbnailUrl = thumbnailUrl;
        this.messageState = messageState;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getReadUrl() {
        return readUrl;
    }

    public String getSenderId() {
        return senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public ChatwalaMessage.MessageState getMessageState() {
        return messageState;
    }
}
