package com.chatwala.android;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import com.chatwala.android.sms.Sms;
import com.chatwala.android.util.CWAnalytics;

/**
 * Created by Eliezer on 3/4/14.
 */
public class SmsSentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.hasExtra(com.chatwala.android.sms.SmsManager.SMS_EXTRA)) {
            Sms sms = intent.getParcelableExtra(com.chatwala.android.sms.SmsManager.SMS_EXTRA);
            if(sms != null) {
                switch(getResultCode()) {
                    case Activity.RESULT_OK:
                        if(sms != null) {

                        }
                        CWAnalytics.sendMessageSentConfirmedEvent(sms.getAnalyticsCategory());
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        if(sms.canRetry()) {
                            long millisecondsToRetryIn = sms.retry();
                            Intent smsRetryIntent = new Intent(com.chatwala.android.sms.SmsManager.SMS_RETRY_ACTION);
                            smsRetryIntent.putExtra(com.chatwala.android.sms.SmsManager.SMS_EXTRA, sms);
                            PendingIntent smsRetryPendingIntent = PendingIntent.getBroadcast(context, sms.hashCode(), smsRetryIntent, 0);
                            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, millisecondsToRetryIn, smsRetryPendingIntent);
                            CWAnalytics.sendMessageSentRetryEvent(sms.getAnalyticsCategory(), sms.getNumRetries());
                        }
                        else {
                            CWAnalytics.sendMessageSentFailedEvent(sms.getAnalyticsCategory());
                        }
                }
            }
            else {
                CWAnalytics.sendMessageSentFailedEvent(null);
            }
        }
    }
}
