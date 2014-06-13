package com.chatwala.android.sms;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.util.CwAnalytics;
import com.chatwala.android.util.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 5:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class SmsManager {
    public static final String DEFAULT_MESSAGE = "I sent you a Chatwala video: ";
    public static final String SMS_EXTRA = "SMS";
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
                    Bundle smsBundle = new Bundle();
                    smsBundle.putParcelable(SMS_EXTRA, sms);
                    smsSentIntent.putExtra(SMS_EXTRA, smsBundle);
                    PendingIntent sentPendingIntent = PendingIntent.getBroadcast(getApp(), messagesSentCount.getAndIncrement(),
                            smsSentIntent, 0);
                    android.telephony.SmsManager.getDefault().sendTextMessage(sms.getNumber(), null,
                            sms.getFullMessage(), sentPendingIntent, null);
                    CwAnalytics.sendMessageSentEvent(sms.getAnalyticsCategory());
                }
                catch(Exception e) {
                    Logger.e("There was an exception while sending SMS(s)", e);
                    CwAnalytics.sendMessageSentFailedEvent(sms.getAnalyticsCategory(), true);
                }
            }
        });
    }
}
