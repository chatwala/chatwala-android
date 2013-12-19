package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.http.GetMessageFileRequest;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 4:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessageFileCommand extends SqliteCommand
{
    private String messageId;

    public GetMessageFileCommand(){}

    public GetMessageFileCommand(String messageId)
    {
        this.messageId = messageId;
    }

    @Override
    public String logSummary()
    {
        return "GetMessageFileCommand";
    }

    @Override
    public boolean same(Command command)
    {
        if(command instanceof GetMessageFileCommand)
        {
            return ((GetMessageFileCommand) command).getMessageId().equals(messageId);
        }
        else
        {
            return false;
        }
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new GetMessageFileRequest(context, messageId).execute();
    }

    public String getMessageId()
    {
        return messageId;
    }
}
