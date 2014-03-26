package com.chatwala.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.Referrer;

import java.net.URLDecoder;

/**
 * Created by Eliezer on 3/6/14.
 */
public class ReferrerReceiver extends BroadcastReceiver {
    public static final String CW_REFERRER_ACTION = "com.chatwala.android.REFERRER";
    public static final String REFERRER_EXTRA = "referrer";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if(intent != null && "com.android.vending.INSTALL_REFERRER".equals(intent.getAction())) {
                String rawReferrer = intent.getStringExtra(REFERRER_EXTRA);
                if(rawReferrer != null) {
                    String referrerStr = URLDecoder.decode(rawReferrer, "UTF-8");
                    Referrer referrer = new Referrer(referrerStr);
                    if(referrer.isValid()) {
                        AppPrefs.getInstance(context).putReferrer(referrerStr);
                        Intent referrerIntent = new Intent(CW_REFERRER_ACTION);
                        referrerIntent.putExtra(REFERRER_EXTRA, referrer);
                        referrerIntent.setPackage(context.getPackageName());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(referrerIntent);
                    }
                }
            }
        }
        catch(Exception e) {
            Logger.e("Couldn't process referrer", e);
        }
    }
}
