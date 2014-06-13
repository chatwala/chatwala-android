package com.chatwala.android.events;

import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.util.CwResult;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/12/2014
 * Time: 2:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaMessageEvent extends BaseChatwalaMessageEvent<ChatwalaMessage> {
    public ChatwalaMessageEvent(String id, CwResult<ChatwalaMessage> message) {
        super(id, message);
    }

    public ChatwalaMessageEvent(String id, int extra, CwResult<ChatwalaMessage> message) {
        super(id, extra, message);
    }

    public ChatwalaMessageEvent(String id, int extra) {
        super(id, extra);
    }
}
