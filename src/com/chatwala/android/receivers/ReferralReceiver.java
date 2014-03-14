package com.chatwala.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.util.CWAnalytics;
import com.chatwala.android.util.Logger;

import java.net.URLDecoder;

/**
 * Created by Eliezer on 3/6/14.
 */
public class ReferralReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if(intent != null && "com.android.vending.INSTALL_REFERRER".equals(intent.getAction())) {
                String rawReferrer = intent.getStringExtra("referrer");
                if(rawReferrer != null) {
                    String referrer = URLDecoder.decode(rawReferrer, "UTF-8");
                    Logger.i("Got referrer - " + referrer);
                    CWAnalytics.sendReferrerReceivedEvent(context, referrer);
                    AppPrefs.getInstance(context).putReferrer(referrer);
                }
            }
        }
        catch(Exception e) {
            Logger.e("Couldn't process referrer", e);
        }
    }
}
