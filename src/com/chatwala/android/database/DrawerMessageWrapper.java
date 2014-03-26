package com.chatwala.android.database;

import com.chatwala.android.adapters.DrawerMessagesAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.chatwala.android.util.Logger;

/**
 * Created by matthewdavis on 1/27/14.
 */
public class DrawerMessageWrapper
{
    private ChatwalaMessage singleMessage;
    private List<DrawerMessageWrapper> wrapperList = null;

    public DrawerMessageWrapper(ChatwalaMessage singleMessage)
    {
        this.singleMessage = singleMessage;
    }

    public DrawerMessageWrapper(ArrayList<ChatwalaMessage> incomingMessages)
    {
        if(incomingMessages.size() > 1)
        {
            Collections.sort(incomingMessages, new Comparator<ChatwalaMessage>() {
                @Override
                public int compare(ChatwalaMessage lhs, ChatwalaMessage rhs) {
                    return lhs.getSortId() - rhs.getSortId();
                }
            });

            wrapperList = new ArrayList<DrawerMessageWrapper>();
            for(ChatwalaMessage message : incomingMessages)
            {
                wrapperList.add(new DrawerMessageWrapper(message));
            }
        }

        this.singleMessage = incomingMessages.get(0);
    }

    public String getMessageId()
    {
        return singleMessage.getMessageId();
    }

    public String getMessageReadUrl() {
        return singleMessage.getReadUrl();
    }

    public String getSenderId()
    {
        return singleMessage.getSenderId();
    }

    public Long getTimestamp()
    {
        return singleMessage.getTimestamp();
    }

    public String getThumbnailUrl() {
        return singleMessage.getThumbnailUrl();
    }

    public ChatwalaMessage.MessageState getMessageState()
    {
        return singleMessage.getMessageState();
    }

    public Integer getSortId()
    {
        return singleMessage.getSortId();
    }

    public boolean isMessageGroup()
    {
        return wrapperList != null;
    }

    public List<DrawerMessageWrapper> getMessageWrapperList()
    {
        return wrapperList;
    }
}
