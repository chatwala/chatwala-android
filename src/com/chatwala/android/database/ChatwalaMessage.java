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
            messageFile = new File(fileUrl);
        }

        return messageFile;
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
}
