package com.chatwala.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.GetMessagesForUserCommand;

/**
 * Created by matthewdavis on 1/27/14.
 */
public class GcmBroadcastReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(final Context context, Intent intent)
    {
        Log.d("#########", "Got a GCM Message");
        DataProcessor.runProcess(new Runnable()
        {
            @Override
            public void run()
            {
                BusHelper.submitCommandSync(context, new GetMessagesForUserCommand());
            }
        });
    }
}
