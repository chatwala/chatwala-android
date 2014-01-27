package com.chatwala.android.database;

/**
 * Created by matthewdavis on 1/27/14.
 */
public class MessageGroup implements DrawerMessageInfo
{
    private String messageId;
    private String senderId;
    private Long timestamp;
    private ChatwalaMessage.MessageState messageState;
    private Integer sortId;

    @Override
    public String getMessageId()
    {
        return messageId;
    }

    @Override
    public String getSenderId()
    {
        return senderId;
    }

    @Override
    public Long getTimestamp()
    {
        return timestamp;
    }

    @Override
    public ChatwalaMessage.MessageState getMessageState()
    {
        return messageState;
    }

    @Override
    public Integer getSortId()
    {
        return sortId;
    }
}
