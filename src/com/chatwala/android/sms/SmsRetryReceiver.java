package com.chatwala.android.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Eliezer on 4/3/2014.
 */
public class SmsRetryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.hasExtra(SmsManager.SMS_EXTRA)) {
            Sms sms = intent.getParcelableExtra(SmsManager.SMS_EXTRA);
            if(sms != null) {
                SmsManager.getInstance().sendSms(sms);
            }
        }
    }
}
