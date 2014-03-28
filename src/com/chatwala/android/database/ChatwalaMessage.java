package com.chatwala.android.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
@DatabaseTable(tableName = "message")
public class ChatwalaMessage {
    @DatabaseField(id = true)
    private String messageId;

    @DatabaseField
    private String url;

    @DatabaseField
    private String senderId;

    @DatabaseField
    private String recipientId;

    @DatabaseField
    private Integer sortId;

    @DatabaseField
    private String thumbnailUrl;

    @DatabaseField
    private String shareUrl;

    @DatabaseField
    private String readUrl;

    private String writeUrl;

    @DatabaseField
    private long threadIndex;

    @DatabaseField
    private String threadId;

    @DatabaseField
    private String groupId;

    @DatabaseField
    private double startRecording;

    @DatabaseField
    private String fileUrl = null;

    private File messageFile = null;

    @DatabaseField
    private Long timestamp;

    @DatabaseField
    private MessageState messageState;

    @DatabaseField
    private String messageMetaDataString;

    @DatabaseField
    private String replyingToMessageId;

    @DatabaseField
    private String imageModifiedSince;

    @DatabaseField
    private String shardKey;

    @DatabaseField
    private boolean walaDownloaded;

    public String getShardKey() {
        return shardKey;
    }

    public void setShardKey(String shardKey) {
        this.shardKey = shardKey;
    }

    public boolean isWalaDownloaded() {
        return walaDownloaded;
    }

    public void setWalaDownloaded(boolean walaDownloaded) {
        this.walaDownloaded = walaDownloaded;
    }

    public String getImageModifiedSince() {
        return imageModifiedSince;
    }

    public void setImageModifiedSince(String imageModifiedSince) {
        this.imageModifiedSince = imageModifiedSince;
    }

    public String getReplyingToMessageId() {
        return replyingToMessageId;
    }

    public void setReplyingToMessageId(String replyingToMessageId) {
        this.replyingToMessageId = replyingToMessageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public Integer getSortId() {
        return sortId;
    }

    public void setSortId(Integer sortId) {
        this.sortId = sortId;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getReadUrl() {
        return readUrl;
    }

    public void setReadUrl(String readUrl) {
        this.readUrl = readUrl;
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

    public void setStartRecording(double startRecording) {
        this.startRecording = startRecording;
    }

    public double getStartRecording() {
        return startRecording;
    }

    public String getWriteUrl() {
        return writeUrl;
    }

    public void setWriteUrl(String writeUrl) {
        this.writeUrl = writeUrl;
    }

    public void populateFromMetaDataJSON(JSONObject message_meta_data) throws JSONException{
        this.setMessageId(message_meta_data.getString("message_id"));
        this.setRecipientId(message_meta_data.getString("recipient_id"));
        this.setSenderId(message_meta_data.getString("sender_id"));
        this.setThumbnailUrl(message_meta_data.getString("thumbnail_url"));
        this.setUrl(message_meta_data.getString("read_url"));
        this.setReadUrl(message_meta_data.getString("read_url"));
        this.setShareUrl(message_meta_data.getString("share_url"));
        this.setGroupId(message_meta_data.getString("group_id"));
        this.setThreadIndex(message_meta_data.getInt("thread_index"));
        this.setThreadId(message_meta_data.getString("thread_id"));
        this.setStartRecording(message_meta_data.getDouble("start_recording"));
        this.setTimestamp(message_meta_data.getLong("timestamp"));
        this.setShardKey(message_meta_data.getString("blob_storage_shard_key"));
        this.setMessageMetaDataString(message_meta_data.toString(4));
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getMessageMetaDataString() {
        return messageMetaDataString;
    }

    public void setMessageMetaDataString(String messageMetaDataString) {
        this.messageMetaDataString = messageMetaDataString;
    }


    public File getMessageFile() {
        if (messageFile == null && fileUrl != null) {
            File temp = new File(fileUrl);
            if (temp.exists()) {
                messageFile = temp;
            }
        }

        return messageFile;
    }

    public String getMessageFileUrl() {
        if (fileUrl == null && messageFile != null) {
            fileUrl = messageFile.getPath();
        }

        return fileUrl;
    }

    public void setMessageFile(File messageFile) {
        if(messageFile != null) {
            this.messageFile = messageFile;
            this.fileUrl = messageFile.getAbsolutePath();
        }
    }

    public void clearMessageFile() {
        this.messageFile = null;
        this.fileUrl = null;
    }

    public Long getTimestamp() {
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

    public enum MessageState {
        UNREAD,
        READ,
        REPLIED
    }
}
