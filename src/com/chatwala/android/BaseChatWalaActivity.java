package com.chatwala.android;

import android.app.Activity;
import android.os.Bundle;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.CheckKillswitchCommand;
import com.google.analytics.tracking.android.EasyTracker;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/11/13
 * Time: 7:40 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseChatWalaActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(!(this instanceof KillswitchActivity))
        {
            ChatwalaApplication.isKillswitchShowing.set(false);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        DataProcessor.runProcess(new Runnable()
        {
            @Override
            public void run()
            {
                BusHelper.submitCommandSync(BaseChatWalaActivity.this, new CheckKillswitchCommand());
            }
        });
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }
}
