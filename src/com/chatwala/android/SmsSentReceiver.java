package com.chatwala.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;
import com.chatwala.android.util.CWAnalytics;

/**
 * Created by Eliezer on 3/4/14.
 */
public class SmsSentReceiver extends BroadcastReceiver {
    private static Toast smsSentToast;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(smsSentToast != null) {
            smsSentToast.cancel();
        }
        switch(getResultCode()) {
            case Activity.RESULT_OK:
                CWAnalytics.sendMessageSentConfirmedEvent();
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
            case SmsManager.RESULT_ERROR_NO_SERVICE:
            case SmsManager.RESULT_ERROR_NULL_PDU:
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                CWAnalytics.sendMessageSentFailedEvent();
                smsSentToast = Toast.makeText(context.getApplicationContext(), "There was an error sending the message", Toast.LENGTH_SHORT);
                smsSentToast.show();
        }
    }
}
