package com.chatwala.android.events;

import com.chatwala.android.messages.ChatwalaSentMessage;
import com.chatwala.android.util.CwResult;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/12/2014
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaSentMessageEvent extends BaseChatwalaMessageEvent<ChatwalaSentMessage> {
    public ChatwalaSentMessageEvent(String id, CwResult<ChatwalaSentMessage> message) {
        super(id, message);
    }

    public ChatwalaSentMessageEvent(String id, int extra, CwResult<ChatwalaSentMessage> message) {
        super(id, extra, message);
    }

    public ChatwalaSentMessageEvent(String id, int extra) {
        super(id, extra);
    }
}
