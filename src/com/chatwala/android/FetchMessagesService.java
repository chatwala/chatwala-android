package com.chatwala.android;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.server20.GetUserInboxCommand;
import com.chatwala.android.util.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/30/13
 * Time: 1:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class FetchMessagesService extends IntentService {
    private static final long ONE_MINUTE = 1000 * 60;

    public FetchMessagesService() {
        super("FetchMessagesService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.i("Got new intent");
        final PendingIntent pi = PendingIntent.getService(this, 0, new Intent(this, FetchMessagesService.class), 0);
        final long minutes = AppPrefs.getInstance(this).getPrefMessageLoadInterval() * ONE_MINUTE;
        final AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        DataProcessor.runProcess(new Runnable() {
            @Override
            public void run() {
                if(!AppPrefs.getInstance(FetchMessagesService.this).getKillswitch().isActive()) {
                    BusHelper.submitCommandSync(FetchMessagesService.this, new GetUserInboxCommand());
                }

                manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + minutes, pi);
            }
        });
    }
}
