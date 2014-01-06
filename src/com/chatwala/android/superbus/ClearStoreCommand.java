package com.chatwala.android.superbus;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.util.CWLog;
import com.chatwala.android.util.MessageDataStore;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/1/14
 * Time: 9:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClearStoreCommand extends SqliteCommand
{
    @Override
    public String logSummary()
    {
        return "ClearStoreCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        if(MessageDataStore.checkClearStore())
        {
            CWLog.i(ClearStoreCommand.class, "Messages deleted");
            Log.d("########", "Messages deleted");
        }
        else
        {
            CWLog.i(ClearStoreCommand.class, "No messages deleted");
            Log.d("########", "No messages deleted");
        }
    }
}
