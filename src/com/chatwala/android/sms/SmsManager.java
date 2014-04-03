package com.chatwala.android.sms;

import android.app.PendingIntent;
import android.content.Intent;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.SmsSentReceiver;
import com.chatwala.android.util.CWAnalytics;
import com.chatwala.android.util.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Eliezer on 4/3/2014.
 */
public class SmsManager {
    public static final String DEFAULT_MESSAGE = "Hey, I sent you a video message on Chatwala: ";
    public static final String SMS_EXTRA = "SMS";
    public static final String SMS_RETRY_ACTION = "com.chatwala.android.SMS_RETRY";
    public static final int MAX_SMS_MESSAGE_LENGTH = 160;

    private static final int NUM_THREADS = 3;

    private ChatwalaApplication app;
    private final ExecutorService queue;
    private final AtomicInteger messagesSentCount = new AtomicInteger(0);

    private final ExecutorService getQueue() {
        return queue;
    }

    private final ChatwalaApplication getApp() {
        return app;
    }

    private SmsManager() {
        queue = Executors.newFixedThreadPool(NUM_THREADS);
    }

    private static class Singleton {
        public static final SmsManager instance = new SmsManager();
    }

    public static SmsManager attachToApp(ChatwalaApplication app) {
        Singleton.instance.app = app;
        return Singleton.instance;
    }

    public static SmsManager getInstance() {
        return Singleton.instance;
    }

    public void sendSms(final Sms sms) {
        getQueue().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent smsSentIntent = new Intent(getApp(), SmsSentReceiver.class);
                    smsSentIntent.putExtra(SMS_EXTRA, sms);
                    PendingIntent sentPendingIntent = PendingIntent.getBroadcast(getApp(), messagesSentCount.getAndIncrement(),
                            smsSentIntent, 0);
                    android.telephony.SmsManager.getDefault().sendTextMessage(sms.getNumber(), null,
                            sms.getFullMessage(), sentPendingIntent, null);
                    CWAnalytics.sendMessageSentEvent(sms.getAnalyticsCategory());
                }
                catch(Exception e) {
                    Logger.e("There was an exception while sending SMS(s)", e);
                    CWAnalytics.sendMessageSentFailedEvent(sms.getAnalyticsCategory());
                }
            }
        });
    }
}
