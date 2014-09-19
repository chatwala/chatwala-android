package com.chatwala.android.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.chatwala.android.queue.jobs.GetUserInboxJob;
import com.chatwala.android.util.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/19/2014
 * Time: 6:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class GcmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(!AppPrefs.getKillswitch().isActive()) {
            Logger.i("Got a GCM message");

            GetUserInboxJob.post();
        }
    }
}
