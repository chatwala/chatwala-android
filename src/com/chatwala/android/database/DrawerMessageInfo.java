package com.chatwala.android.database;

/**
 * Created by matthewdavis on 1/27/14.
 */
public interface DrawerMessageInfo
{
    public String getMessageId();
    public String getSenderId();
    public Long getTimestamp();
    public ChatwalaMessage.MessageState getMessageState();
    public Integer getSortId();
}
