package com.chatwala.android.superbus.server20;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.http.server20.GetUserInboxRequest;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 4:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserInboxCommand extends SqliteCommand
{
    @Override
    public String logSummary()
    {
        return "GetUserInboxCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return command instanceof GetUserInboxCommand;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new GetUserInboxRequest(context).execute();
    }
}
