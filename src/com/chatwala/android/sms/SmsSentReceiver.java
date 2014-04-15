package com.chatwala.android.sms;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import com.chatwala.android.util.CWAnalytics;

/**
 * Created by Eliezer on 3/4/14.
 */
public class SmsSentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String SMS_EXTRA_KEY = com.chatwala.android.sms.SmsManager.SMS_EXTRA;
        if(intent.hasExtra(SMS_EXTRA_KEY)) {
            Bundle smsBundle = intent.getBundleExtra(SMS_EXTRA_KEY);
            Sms sms = null;
            if(smsBundle != null && smsBundle.containsKey(SMS_EXTRA_KEY)) {
                sms = smsBundle.getParcelable(SMS_EXTRA_KEY);
            }

            switch(getResultCode()) {
                case Activity.RESULT_OK:
                    if (sms != null) {
                        CWAnalytics.sendMessageSentConfirmedEvent(sms.getAnalyticsCategory());
                    } else {
                        CWAnalytics.sendMessageSentConfirmedEvent(null);
                    }
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                case SmsManager.RESULT_ERROR_NULL_PDU:
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                default:
                    if (sms != null) {
                        /*if (sms.canRetry()) {
                            long millisecondsToRetryIn = sms.retry();
                            Intent smsRetryIntent = new Intent(context, SmsRetryReceiver.class);
                            smsRetryIntent.putExtra(com.chatwala.android.sms.SmsManager.SMS_EXTRA, sms);
                            PendingIntent smsRetryPendingIntent = PendingIntent.getBroadcast(context, sms.hashCode(),
                                    smsRetryIntent, 0);
                            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            am.set(AlarmManager.RTC_WAKEUP, new Date().getTime() + millisecondsToRetryIn, smsRetryPendingIntent);
                        }
                        else {
                            CWAnalytics.sendMessageSentFailedEvent(sms.getAnalyticsCategory(), false);
                        }*/
                        CWAnalytics.sendMessageSentFailedEvent(sms.getAnalyticsCategory(), false);
                    }
                    else {
                        CWAnalytics.sendMessageSentFailedEvent(null, false);
                    }
            }
        }
    }
}
