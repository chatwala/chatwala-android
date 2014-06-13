package com.chatwala.android.messages;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 6/1/2014
 * Time: 2:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaMessageThread {
    private ArrayList<ChatwalaMessage> messages;
    private ArrayList<ChatwalaSentMessage> sentMessages;

    private String threadId;
    private String senderId;
    private String recipientId;

    public ChatwalaMessageThread(ChatwalaMessageBase message) {
        this.threadId = message.getThreadId();
        this.senderId = message.getSenderId();
        this.recipientId = message.getRecipientId();

        messages = new ArrayList<ChatwalaMessage>();
        sentMessages = new ArrayList<ChatwalaSentMessage>();
    }

    public ArrayList<ChatwalaMessage> getMessages() {
        return messages;
    }

    public ArrayList<ChatwalaSentMessage> getSentMessages() {
        return sentMessages;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public int getMessageCount() {
        return messages.size() + sentMessages.size();
    }
}
