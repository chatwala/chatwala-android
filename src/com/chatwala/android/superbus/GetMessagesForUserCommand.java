package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.http.GetMessagesForUserRequest;
import com.chatwala.android.loaders.BroadcastSender;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 4:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessagesForUserCommand extends SqliteCommand
{
    @Override
    public String logSummary()
    {
        return "GetMessagesForUserCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return command instanceof GetMessagesForUserCommand;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new GetMessagesForUserRequest(context).execute();
    }

    @Override
    public void onSuccess(Context context)
    {
        BroadcastSender.makeNewMessagesBroadcast(context);
    }
}
