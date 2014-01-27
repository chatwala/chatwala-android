package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.http.PostRegisterGCMRequest;

/**
 * Created by matthewdavis on 1/27/14.
 */
public class PostRegisterGCMCommand extends SqliteCommand
{
    @Override
    public String logSummary()
    {
        return "PostRegisterGCMCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return command instanceof PostRegisterGCMCommand;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new PostRegisterGCMRequest(context).execute();
    }
}
