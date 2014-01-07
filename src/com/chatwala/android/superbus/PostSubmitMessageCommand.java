package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.http.PostSubmitMessageRequest;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostSubmitMessageCommand extends SqliteCommand
{
    String localMessagePath;
    String recipientId, originalMessageId;

    public PostSubmitMessageCommand(){}

    public PostSubmitMessageCommand(String localMessagePath, String recipientId, String originalMessageId)
    {
        this.localMessagePath = localMessagePath;
        this.recipientId = recipientId;
        this.originalMessageId = originalMessageId;
    }

    @Override
    public String logSummary()
    {
        return "PostSubmitMessageCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new PostSubmitMessageRequest(context, localMessagePath, recipientId, originalMessageId).execute();
    }
}
