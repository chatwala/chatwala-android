package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.http.PostFinalizeMessageRequest;

/**
 * Created by matthewdavis on 1/31/14.
 */
public class PostFinalizeMessageCommand extends SqliteCommand
{
    String messageId, senderId, receiverId;

    public PostFinalizeMessageCommand() {}

    public PostFinalizeMessageCommand(String messageId, String senderId, String receiverId)
    {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    @Override
    public String logSummary()
    {
        return "PostFinalizeMessageCommand";
    }

    @Override
    public boolean same(Command command)
    {
        if(command instanceof PostFinalizeMessageCommand)
        {
            return messageId.equals(((PostFinalizeMessageCommand)command).getMessageId());
        }
        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new PostFinalizeMessageRequest(context, messageId, senderId, receiverId).execute();
    }

    public String getMessageId()
    {
        return messageId;
    }

    @Override
    public int getPriority()
    {
        return HIGHER_PRIORITY;
    }
}
