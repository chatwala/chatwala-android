package com.chatwala.android.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
@DatabaseTable(tableName = "message")
public class ChatwalaMessage
{
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

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private MessageMetadata messageMetadata;

    @DatabaseField
    private String fileUrl = null;

    private File messageFile = null;

    @DatabaseField
    private Long timestamp;

    @DatabaseField
    private MessageState messageState;


    public String getMessageId()
    {
        return messageId;
    }

    public void setMessageId(String messageId)
    {
        this.messageId = messageId;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getSenderId()
    {
        return senderId != null ? senderId : messageMetadata.senderId;
    }

    public void setSenderId(String senderId)
    {
        this.senderId = senderId;
    }

    public String getRecipientId()
    {
        return recipientId != null ? recipientId : messageMetadata.recipientId;
    }

    public void setRecipientId(String recipientId)
    {
        this.recipientId = recipientId;
    }

    public Integer getSortId()
    {
        return sortId;
    }

    public void setSortId(Integer sortId)
    {
        this.sortId = sortId;
    }

    public String getThumbnailUrl()
    {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl)
    {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void initMetadata(JSONObject metadataJson) throws JSONException
    {
        messageMetadata = new MessageMetadata();
        messageMetadata.init(metadataJson);
    }

    public void saveMetadata(DatabaseHelper helper) throws SQLException
    {
        helper.getMessageMetadataDao().createOrUpdate(messageMetadata);
    }

    public double getStartRecording()
    {
        return messageMetadata != null ? messageMetadata.startRecording : 0;
    }

    public MessageMetadata copyOrMakeNewMetadata()
    {
        return messageMetadata != null ? messageMetadata.copy() : new MessageMetadata();
    }

    public File getMessageFile()
    {
        if(messageFile == null && fileUrl != null)
        {
            File temp = new File(fileUrl);
            if(temp.exists())
            {
                messageFile = temp;
            }
        }

        return messageFile;
    }

    public String getMessageFileUrl()
    {
        if(fileUrl == null && messageFile != null)
        {
            fileUrl = messageFile.getPath();
        }

        return fileUrl;
    }

    public void setMessageFile(File messageFile)
    {
        this.messageFile = messageFile;
        this.fileUrl = messageFile.getAbsolutePath();
    }

    public void clearMessageFile()
    {
        this.messageFile = null;
        this.fileUrl = null;
    }

    public Long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Long timestamp)
    {
        this.timestamp = timestamp;
    }

    public MessageState getMessageState()
    {
        return messageState;
    }

    public void setMessageState(MessageState messageState)
    {
        this.messageState = messageState;
    }

    public enum MessageState {
        UNREAD,
        READ,
        REPLIED
    }
}
