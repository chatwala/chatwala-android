package com.chatwala.android;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    protected void onStart()
    {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }
}
