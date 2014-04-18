package com.chatwala.android.superbus.server20;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.networking.NetworkManager;
import com.chatwala.android.networking.requests.DeleteMessageRequest;

/**
 * Created by Eliezer on 4/10/2014.
 */
public class DeleteMessageCommand extends SqliteCommand {
    private ChatwalaMessage message;

    public DeleteMessageCommand(){}

    public DeleteMessageCommand(ChatwalaMessage message)
    {
        this.message = message;
    }

    @Override
    public String logSummary()
    {
        return "DeleteMessageCommand";
    }

    @Override
    public boolean same(Command command)
    {
        if(command instanceof DeleteMessageCommand)
        {
            if(message.getMessageId().equals(((DeleteMessageCommand) command).getMessage().getMessageId()))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException {
        NetworkManager.getInstance().postToQueue(new DeleteMessageRequest(getMessage()).getCallable(context, 3));
    }

    @Override
    public void onSuccess(Context context) {
        try {
            message.getMessageFile().delete();
        }
        catch(Exception e) {}

        try {

            DatabaseHelper.getInstance(context).getChatwalaMessageDao().delete(message);
        }
        catch(Exception e) {}
    }

    public ChatwalaMessage getMessage()
    {
        return message;
    }
}
