package com.chatwala.android;

import android.content.Context;
import android.content.SharedPreferences;
import com.chatwala.android.activity.SettingsActivity;

/**
 * Created by Eliezer on 3/6/14.
 */
public class UpdateManager {
    private static final String UPDATE_PREFS = "UPDATE_PREFS";
    private static final String IS_UPDATE_KEY = "IS_UPDATE_%d";
    private static boolean isUpdate(Context context, int vc) {
        SharedPreferences sp = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE);
        if(!sp.getBoolean(String.format(IS_UPDATE_KEY, vc), false)) {
            sp.edit().putBoolean(String.format(IS_UPDATE_KEY, vc), true).apply();
            return true;
        }
        else {
            return false;
        }
    }

    public static void handleUpdateIfNeeded(Context context) {
        int vc;
        try {
            vc = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch(Exception e) { return; }

        if(isUpdate(context, vc)) {
            AppPrefs prefs = AppPrefs.getInstance(context);
            switch(vc) {
                case 20014:
                    do20014Update(context, prefs);
                    break;
                default:
                    break;
            }
        }
    }

    private static void do20014Update(Context context, AppPrefs prefs) {
        if(prefs.getDeliveryMethod() == SettingsActivity.DeliveryMethod.SMS) { //otherwise if they use SMS, switch them to CWSMS
            prefs.setDeliveryMethod(SettingsActivity.DeliveryMethod.CWSMS);
        }
    }

}
