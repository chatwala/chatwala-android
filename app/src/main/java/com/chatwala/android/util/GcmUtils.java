package com.chatwala.android.util;

import android.content.Context;
import com.chatwala.android.app.AppPrefs;
import com.chatwala.android.app.ChatwalaApplication;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/19/2014
 * Time: 5:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class GcmUtils {
    public static boolean shouldRegisterForGcm(Context context) {
        return checkForPlayServices(context) && shouldRefreshGcmToken(context);
    }

    private static boolean checkForPlayServices(Context context) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if(resultCode != ConnectionResult.SUCCESS && resultCode != ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Logger.w("Can't connect to Play Services, but can recover");
            }
            else {
                Logger.e("Can't connect to Play Services");
            }
            return false;
        }
        return true;
    }

    private static boolean shouldRefreshGcmToken(Context context) {
        String currentToken = AppPrefs.getGcmToken();
        if(currentToken == null) {
            return true;
        }
        else {
            int registeredVersion = AppPrefs.getGcmTokenVersion();
            int currentVersion = ChatwalaApplication.getVersionCode();
            if(currentVersion == -1) {
                //be safe and refresh the token
                return true;
            }
            return currentVersion > registeredVersion;
        }
    }
}
