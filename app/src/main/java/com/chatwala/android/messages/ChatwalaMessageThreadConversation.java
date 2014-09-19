package com.chatwala.android.messages;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 6/1/2014
 * Time: 2:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaMessageThreadConversation extends ChatwalaMessageThread {
    private ArrayList<Integer> messageOffsets;
    private ArrayList<Integer> sentMessageOffsets;

    public ChatwalaMessageThreadConversation(ChatwalaMessageBase message) {
        super(message);

        messageOffsets = new ArrayList<Integer>();
        sentMessageOffsets = new ArrayList<Integer>();
    }

    public ArrayList<Integer> getMessageOffsets() {
        return messageOffsets;
    }

    public ArrayList<Integer> getSentMessageOffsets() {
        return sentMessageOffsets;
    }
}
