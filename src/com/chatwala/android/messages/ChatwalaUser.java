package com.chatwala.android.messages;

import com.chatwala.android.files.FileManager;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 12:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaUser {
    private String senderId;
    private long timestamp;
    private String thumbnailUrl;
    private boolean isUnread;

    public ChatwalaUser(String senderId, long timestamp, String thumbnailUrl, boolean isUnread) {
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

    public File getLocalThumb() {
        return FileManager.getUserThumb(senderId);
    }
}
