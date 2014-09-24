package com.chatwala.android.messages;

import android.os.Parcel;
import android.os.Parcelable;
import com.chatwala.android.util.Logger;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import org.json.JSONObject;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 10:33 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ChatwalaMessageBase implements Parcelable {
    public ChatwalaMessageBase() {}

    public ChatwalaMessageBase(String messageId) {
        this.messageId = messageId;
    }

    public ChatwalaMessageBase(JSONObject metadata) {
        populateFromMetadata(metadata);
    }

    public ChatwalaMessageBase(Parcel p) {
        messageId = p.readString();
        senderId = p.readString();
        recipientId = p.readString();
        sortId = p.readInt();
        imageUrl = p.readString();
        imageModifiedSince = p.readString();
        userImageModifiedSince = p.readString();
        userImageUrl = p.readString();
        readUrl = p.readString();
        shareUrl = p.readString();
        shardKey = p.readString();
        threadIndex = p.readLong();
        threadId = p.readString();
        groupId = p.readString();
        startRecording = p.readDouble();
        timestamp = p.readLong();
        try {
            messageState = MessageState.valueOf(p.readString());
        }
        catch(Exception e) {
            messageState = MessageState.UNREAD;
        }
        messageMetadataString = p.readString();
        replyingToMessageId = p.readString();
        walaDownloaded = p.readInt() == 1;
        isDeleted = p.readInt() == 1;
    }

    @DatabaseField(id = true)
    private String messageId;

    @DatabaseField
    private String senderId;

    @DatabaseField
    private String recipientId;

    @DatabaseField
    private int sortId;

    @DatabaseField
    private String imageUrl;

    @DatabaseField
    private String imageModifiedSince;

    @DatabaseField
    private String userImageModifiedSince;

    @DatabaseField
    private String userImageUrl;

    @DatabaseField
    private String readUrl;

    @DatabaseField
    private String shareUrl;

    @DatabaseField
    private String shardKey;

    @DatabaseField
    private long threadIndex;

    @DatabaseField
    private String threadId;

    @DatabaseField
    private String groupId;

    @DatabaseField
    private double startRecording;

    @DatabaseField
    private long timestamp;

    @DatabaseField
    private MessageState messageState;

    @DatabaseField
    private String messageMetadataString;

    @DatabaseField
    private String replyingToMessageId;

    @DatabaseField
    private boolean walaDownloaded;

    @DatabaseField
    private boolean isDeleted;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public int getSortId() {
        return sortId;
    }

    public void setSortId(Integer sortId) {
        this.sortId = sortId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageModifiedSince() {
        return imageModifiedSince;
    }

    public void setImageModifiedSince(String imageModifiedSince) {
        this.imageModifiedSince = imageModifiedSince;
    }

    public String getUserImageModifiedSince() {
        return userImageModifiedSince;
    }

    public void setUserImageModifiedSince(String userImageModifiedSince) {
        this.userImageModifiedSince = userImageModifiedSince;
    }

    public String getUserImageUrl() {
        return userImageUrl;
    }

    public void setUserImageUrl(String userImageUrl) {
        this.userImageUrl = userImageUrl;
    }

    public String getReadUrl() {
        return readUrl;
    }

    public void setReadUrl(String readUrl) {
        this.readUrl = readUrl;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getShardKey() {
        return shardKey;
    }

    public void setShardKey(String shardKey) {
        this.shardKey = shardKey;
    }

    public long getThreadIndex() {
        return threadIndex;
    }

    public void setThreadIndex(long threadIndex) {
        this.threadIndex = threadIndex;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public double getStartRecording() {
        return startRecording;
    }

    public void setStartRecording(double startRecording) {
        this.startRecording = startRecording;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public MessageState getMessageState() {
        return messageState;
    }

    public void setMessageState(MessageState messageState) {
        this.messageState = messageState;
    }

    public String getMessageMetadataString() {
        return messageMetadataString;
    }

    public void setMessageMetadataString(String messageMetadataString) {
        this.messageMetadataString = messageMetadataString;
    }

    public String getReplyingToMessageId() {
        return replyingToMessageId;
    }

    public void setReplyingToMessageId(String replyingToMessageId) {
        this.replyingToMessageId = replyingToMessageId;
    }

    public boolean isWalaDownloaded() {
        return walaDownloaded;
    }

    public void setWalaDownloaded(boolean walaDownloaded) {
        this.walaDownloaded = walaDownloaded;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void populateFromMetadata(JSONObject metadata) {
        try {
            this.setMessageId(metadata.getString(MessageMetadataKeys.ID));
            this.setRecipientId(metadata.getString(MessageMetadataKeys.RECIPIENT_ID));
            this.setSenderId(metadata.getString(MessageMetadataKeys.SENDER_ID));
            this.setImageUrl(metadata.getString(MessageMetadataKeys.IMAGE_URL));
            this.setThreadIndex(metadata.getInt(MessageMetadataKeys.THREAD_INDEX));
            this.setThreadId(metadata.getString(MessageMetadataKeys.THREAD_ID));
            this.setStartRecording(metadata.getDouble(MessageMetadataKeys.START_RECORDING));
            this.setTimestamp(metadata.getLong(MessageMetadataKeys.TIMESTAMP));
            this.setShardKey(metadata.getString(MessageMetadataKeys.SHARD_KEY));
            this.setReadUrl(metadata.getString(MessageMetadataKeys.READ_URL));
            this.setGroupId(metadata.getString(MessageMetadataKeys.GROUP_ID));
            this.setUserImageUrl(metadata.optString(MessageMetadataKeys.USER_IMAGE_URL, getImageUrl()));
            this.setShareUrl(metadata.optString(MessageMetadataKeys.SHARE_URL, ""));
        }
        catch(Exception e) {
            Logger.e("Got error while trying to parse old wala file (messageId = " + getMessageId(), e);
        }
        finally {
            this.setMessageMetadataString(metadata.toString());
        }
    }

    public boolean isInLocalStorage() {
        return getLocalVideoFile() != null && getLocalVideoFile().exists();
    }

    public abstract File getLocalVideoFile();

    public abstract File getLocalMetadataFile();

    public abstract File getLocalWalaFile();

    public abstract File getLocalMessageImage();

    public abstract File getLocalMessageThumb();

    public abstract File getLocalUserImage();

    public abstract File getLocalUserThumb();

    public abstract <T extends ChatwalaMessageBase> Dao<T, String> getDao() throws Exception;

    public void deleteAllLocalFiles() {
        getLocalVideoFile().delete();
        getLocalMetadataFile().delete();
        getLocalMessageThumb().delete();
        getLocalWalaFile().delete();
    }

    //////////////////////////////
    // Parcelable apis
    //////////////////////////////
    public void writeToParcel(Parcel p, int flags) {
        p.writeString(messageId);
        p.writeString(senderId);
        p.writeString(recipientId);
        p.writeInt(sortId);
        p.writeString(imageUrl);
        p.writeString(imageModifiedSince);
        p.writeString(userImageModifiedSince);
        p.writeString(userImageUrl);
        p.writeString(readUrl);
        p.writeString(shareUrl);
        p.writeString(shardKey);
        p.writeLong(threadIndex);
        p.writeString(threadId);
        p.writeString(groupId);
        p.writeDouble(startRecording);
        p.writeLong(timestamp);
        p.writeString(messageState.name());
        p.writeString(messageMetadataString);
        p.writeString(replyingToMessageId);
        p.writeInt((walaDownloaded ? 1 : 0));
        p.writeInt((isDeleted ? 1 : 0));
    }
    //////////////////////////////
    // end Parcelable apis
    //////////////////////////////

}
