package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.ChatwalaNotificationManager;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.http.GetMessageFileRequest;
import com.chatwala.android.loaders.BroadcastSender;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 4:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessageFileCommand extends SqliteCommand
{
    private ChatwalaMessage messageMetadata;

    public GetMessageFileCommand(){}

    public GetMessageFileCommand(ChatwalaMessage message)
    {
        this.messageMetadata = message;
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
            return ((GetMessageFileCommand) command).getMessageId().equals(getMessageId());
        }
        else
        {
            return false;
        }
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new GetMessageFileRequest(context, messageMetadata).execute();
    }

    @Override
    public void onSuccess(Context context)
    {
        BroadcastSender.makeNewMessagesBroadcast(context);
        ChatwalaNotificationManager.makeNewMessagesNotification(context);
    }

    public String getMessageId()
    {
        return messageMetadata.getMessageId();
    }
}
