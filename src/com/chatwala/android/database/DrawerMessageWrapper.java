package com.chatwala.android.database;

import com.chatwala.android.adapters.DrawerConversationsAdapter;

/**
 * Created by matthewdavis on 1/27/14.
 */
public interface DrawerMessageWrapper
{
    //private ChatwalaMessage singleMessage = null;


    public String getMessageId();
    public String getSenderId();
    public Long getTimestamp();
    public ChatwalaMessage.MessageState getMessageState();
    public Integer getSortId();
}
