package com.chatwala.android.activity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ViewGroup;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.EnvironmentVariables;
import com.chatwala.android.util.Logger;
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

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... v) {
                if(BaseChatWalaActivity.this != null) {
                    Logger.i("About to send Facebook activateApp event");
                    com.facebook.AppEventsLogger.activateApp(BaseChatWalaActivity.this, EnvironmentVariables.get().getFacebookAppId());
                }
                else {
                    Logger.e("Couldn't send Facebook activateApp event because Activity was null");
                }
                return null;
            }
        }.execute();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    public ViewGroup findViewRoot()
    {
        return (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
    }
}
