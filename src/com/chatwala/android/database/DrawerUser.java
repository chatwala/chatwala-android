package com.chatwala.android.database;

/**
 * Created by Eliezer on 3/25/2014.
 */
public class DrawerUser {
    private String senderId;
    private long timestamp;
    private String thumbnailUrl;
    private boolean isUnread;

    public DrawerUser(String senderId, long timestamp, String thumbnailUrl, boolean isUnread) {
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.thumbnailUrl = thumbnailUrl;
        this.isUnread = isUnread;
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

    public boolean isUnread() {
        return isUnread;
    }
}
