package com.chatwala.android.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.Referrer;

import java.net.URLDecoder;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/19/2014
 * Time: 6:12 PM
 * To change this template use File | Settings | File Templates.
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
                        AppPrefs.putReferrer(referrer);
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
