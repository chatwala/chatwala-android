package com.chatwala.android.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/12/13
 * Time: 11:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class SharedPrefsUtils
{
    private static final String FIRST_OPEN = "FIRST_OPEN";
    private static final String USER_ID = "USER_ID";

    private static final String PREFERENCES_KEY = "CHATWALA_PREFERENCES";
    private static SharedPreferences sharedPreferences = null;

    public static Boolean isFirstOpen(Context context)
    {
        Boolean firstOpen = getBooleanValue(context, FIRST_OPEN, true);
        setBooleanValue(context, FIRST_OPEN, false);
        return firstOpen;
    }

    public static void setUserId(Context context, String userId)
    {
        setStringValue(context, USER_ID, userId);
    }

    public static String getUserId(Context context)
    {
        return getStringValue(context, USER_ID, null);
    }

    private static SharedPreferences getPrefs(Context context)
    {
        if (sharedPreferences == null)
        {
            sharedPreferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        }
        return sharedPreferences;
    }

    private static Boolean getBooleanValue(Context context, String key, Boolean defaultVal)
    {
        return getPrefs(context).getBoolean(key, defaultVal);
    }

    private static boolean setBooleanValue(Context context, String key, Boolean value)
    {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    private static String getStringValue(Context context, String key, String defaultVal)
    {
        return getPrefs(context).getString(key, defaultVal);
    }

    private static boolean setStringValue(Context context, String key, String value)
    {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(key, value);
        return editor.commit();
    }
}
