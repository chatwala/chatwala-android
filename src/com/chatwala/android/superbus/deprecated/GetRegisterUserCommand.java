package com.chatwala.android.superbus.deprecated;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.http.deprecated.GetRegisterUserRequest;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 1:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetRegisterUserCommand extends SqliteCommand
{
    @Override
    public String logSummary()
    {
        return "GetRegisterUserCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return command instanceof GetRegisterUserCommand;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new GetRegisterUserRequest(context).execute();
    }
}
