package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.http.PostRegisterPushTokenRequest;

/**
 * Created by matthewdavis on 1/30/14.
 */
public class PostRegisterPushTokenCommand extends SqliteCommand
{
    @Override
    public String logSummary()
    {
        return "PostRegisterPushTokenCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return command instanceof PostRegisterPushTokenCommand;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new PostRegisterPushTokenRequest(context).execute();
    }
}
