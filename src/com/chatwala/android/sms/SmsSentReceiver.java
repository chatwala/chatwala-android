package com.chatwala.android.sms;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.chatwala.android.util.CwAnalytics;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 5:22 PM
 * To change this template use File | Settings | File Templates.
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
                        CwAnalytics.sendMessageSentConfirmedEvent(sms.getAnalyticsCategory());
                    }
                    else {
                        CwAnalytics.sendMessageSentConfirmedEvent(null);
                    }
                    break;
                case android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                case android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE:
                case android.telephony.SmsManager.RESULT_ERROR_NULL_PDU:
                case android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF:
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
                        CwAnalytics.sendMessageSentFailedEvent(sms.getAnalyticsCategory(), false);
                    }
                    else {
                        CwAnalytics.sendMessageSentFailedEvent(null, false);
                    }
            }
        }
    }
}
