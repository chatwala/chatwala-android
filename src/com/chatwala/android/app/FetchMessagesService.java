package com.chatwala.android.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import com.chatwala.android.queue.jobs.GetUserInboxJob;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/19/2014
 * Time: 6:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class FetchMessagesService extends Service {
    private static final long TWO_HOURS = ((1000 * 60) * 60) * 2;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final PendingIntent pi = PendingIntent.getService(this, 0, new Intent(this, FetchMessagesService.class), 0);
        final AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if(!AppPrefs.getKillswitch().isActive()) {
            GetUserInboxJob.post();
        }

        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TWO_HOURS, pi);

        return START_STICKY;
    }
}
