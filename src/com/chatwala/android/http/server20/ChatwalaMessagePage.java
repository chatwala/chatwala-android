package com.chatwala.android.http.server20;
import java.util.ArrayList;
import com.chatwala.android.database.ChatwalaMessage;

/**
 * Created by samirahman on 3/16/14.
 */
public class ChatwalaMessagePage {
    ArrayList<ChatwalaMessage> messages;
    boolean continuePaging;
    String continueId;

    public ArrayList<ChatwalaMessage> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<ChatwalaMessage> messages) {
        this.messages = messages;
    }

    public boolean shouldContinuePaging() {
        return continuePaging;
    }

    public void setContinuePaging(boolean continuePaging) {
        this.continuePaging = continuePaging;
    }

    public String getContinueId() {
        return continueId;
    }

    public void setContinueId(String continueId) {
        this.continueId = continueId;
    }
}
