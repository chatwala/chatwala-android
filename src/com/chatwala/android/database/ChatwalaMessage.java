package com.chatwala.android.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

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
}
