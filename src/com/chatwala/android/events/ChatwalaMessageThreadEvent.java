package com.chatwala.android.events;

import com.chatwala.android.messages.ChatwalaMessageThreadConversation;
import com.chatwala.android.util.CwResult;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 6/1/2014
 * Time: 2:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaMessageThreadEvent extends ResultEvent<ChatwalaMessageThreadConversation> {
    public ChatwalaMessageThreadEvent(String id, CwResult<ChatwalaMessageThreadConversation> messages) {
        super(id, messages);
    }

    public ChatwalaMessageThreadEvent(String id, int extra, CwResult<ChatwalaMessageThreadConversation> messages) {
        super(id, extra, messages);
    }

    public ChatwalaMessageThreadEvent(String id, int extra) {
        super(id, extra);
    }
}
