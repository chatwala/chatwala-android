package com.chatwala.android.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import com.chatwala.android.AppPrefs;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created by matthewdavis on 1/28/14.
 */
public class GCMUtils
{
    public static boolean shouldRegisterForGcm(Context context)
    {
        return (checkPlayServices(context) && shouldRefreshGcmToken(context));
    }

    private static boolean checkPlayServices(Context context)
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
            {
                Log.d("############", "Play services is present but may be supported.");
                CWLog.b(GCMUtils.class, "Play services is not present but may be supported.");
                //GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            else
            {
                Log.d("############", "Play services is not supported.");
                CWLog.b(GCMUtils.class, "Play services is not supported.");
            }
            return false;
        }
        return true;
    }

    private static boolean shouldRefreshGcmToken(Context context)
    {
        String currentToken = AppPrefs.getInstance(context).getGcmToken();
        if(currentToken == null)
        {
            return true;
        }
        else
        {
            int registeredVersion = AppPrefs.getInstance(context).getGcmAppVersion();
            int currentVersion = 0;

            try
            {
                getAppVersion(context);
            }
            catch (Exception e)
            {
                currentVersion = Integer.MAX_VALUE;
            }

            return (currentVersion > registeredVersion);
        }
    }

    public static int getAppVersion(Context context) throws Exception
    {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return packageInfo.versionCode;
    }
}
