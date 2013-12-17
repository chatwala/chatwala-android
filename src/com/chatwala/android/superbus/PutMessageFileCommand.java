package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.http.PutMessageFileRequest;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class PutMessageFileCommand extends SqliteCommand
{
    private String messageId, messageLocalUrl;

    public PutMessageFileCommand(){}

    public PutMessageFileCommand(String messageId, String messageLocalUrl)
    {
        this.messageId = messageId;
        this.messageLocalUrl = messageLocalUrl;
    }

    @Override
    public String logSummary()
    {
        return "PutMessageFileCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new PutMessageFileRequest(context, messageLocalUrl, messageId).execute();
    }
}
