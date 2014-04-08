package com.chatwala.android.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.chatwala.android.util.CWAnalytics;

/**
 * Created by Eliezer on 4/3/2014.
 */
public class SmsRetryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.hasExtra(SmsManager.SMS_EXTRA)) {
            String extraKey = com.chatwala.android.sms.SmsManager.SMS_EXTRA;
            Bundle smsBundle = intent.getBundleExtra(extraKey);
            Sms sms = null;
            if(smsBundle != null && smsBundle.containsKey(extraKey)) {
                sms = smsBundle.getParcelable(extraKey);
            }
            if(sms != null) {
                CWAnalytics.sendMessageSentRetryEvent(sms.getAnalyticsCategory(), sms.getNumRetries() + 1);
                SmsManager.getInstance().sendSms(sms);
            }
        }
    }
}
