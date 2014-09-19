package com.chatwala.android.app;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.chatwala.android.util.DeliveryMethod;
import com.chatwala.android.util.KillswitchInfo;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.Referrer;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class AppPrefs {
    private static ChatwalaApplication app;

    public static void initPrefs(ChatwalaApplication app) {
        AppPrefs.app = app;
    }

    private static final String PREF_IS_FIRST_OPEN = "IS_FIRST_OPEN";
    private static final String PREF_USER_ID = "USER_ID";
    private static final String PREF_FIRST_LINK = "FIRST_LINK";
    private static final String PREF_USER_PROFILE_PIC_READ_URL = "USER_PROFILE_PIC_READ_URL";
    private static final String PREF_USER_PROFILE_PIC_LAST_MODIFIED = "USER_PROFILE_PIC_LAST_MODIFIED";
    private static final String PREF_GCM_TOKEN = "GCM_TOKEN";
    private static final String PREF_GCM_TOKEN_VERSION = "GCM_TOKEN_VERSION";
    private static final String PREF_REFERRER = "REFERRER";
    private static final String PREF_FIRST_BUTTON_PRESS = "FIRST_BUTTON_PRESS";
    private static final String PREF_SHOWED_TOP_CONTACTS = "SHOWED_TOP_CONTACT";
    private static final String PREF_KILLSWITCH = "KILLSWITCH";
    private static final String PREF_SHOW_PREVIEW = "SHOW_PREVIEW";
    private static final String PREF_DELIVERY_METHOD = "DELIVERY_METHOD";
    private static final String PREF_MESSAGE_REFRESH_INTERVAL = "MESSAGE_REFRESH_INTERVAL";
    private static final String PREF_MAX_DISK_SPACE = "MAX_DISK_SPACE";

    private static final int DEFAULT_DELIVERY_METHOD = 1;
    private static final long DEFAULT_MESSAGE_REFRESH_INTERVAL = 60 * 60 * 60 * 1000; //one hour
    private static final int DEFAULT_MAX_DISK_SPACE = 500; //mb

    //TODO LEGACY take out when no longer needed (enough people migrated to 2.0)
    public static final String LEGACY_PREF_IS_FIRST_OPEN = "FIRST_OPEN";

    /*package*/ static boolean isFirstOpen() {
        //TODO LEGACY take out when no longer needed (enough people migrated to 2.0)
        if(Prefs.contains(LEGACY_PREF_IS_FIRST_OPEN)) {
            return false;
        }
        boolean isFirstOpen = Prefs.getBoolean(PREF_IS_FIRST_OPEN, true);
        if(isFirstOpen) {
            Prefs.edit()
                    .putBoolean(PREF_FIRST_BUTTON_PRESS, false)
                    .putBoolean(PREF_SHOWED_TOP_CONTACTS, false).apply();
        }
        Prefs.putBoolean(PREF_IS_FIRST_OPEN, false);
        return isFirstOpen;
    }

    public static String getUserId() {
        return Prefs.getString(PREF_USER_ID, null);
    }

    public static String getFirstLink() {
        return Prefs.getString(PREF_FIRST_LINK, null);
    }

    public static void setFirstLink(String firstLink) {
        Prefs.putString(PREF_FIRST_LINK, firstLink);
    }

    public static String getUserProfilePicReadUrl() {
        return Prefs.getString(PREF_USER_PROFILE_PIC_READ_URL, null);
    }

    public static void setUserProfilePicReadUrl(String userProfilePicReadUrl) {
        Prefs.putString(PREF_USER_PROFILE_PIC_READ_URL, userProfilePicReadUrl);
    }

    public static String getUserProfilePicLastModified() {
        return Prefs.getString(PREF_USER_PROFILE_PIC_LAST_MODIFIED, null);
    }

    public static void setUserProfilePicLastModified(String userProfilePicLastModified) {
        Prefs.putString(PREF_USER_PROFILE_PIC_LAST_MODIFIED, userProfilePicLastModified);
    }

    public static void setUserId(String userId) {
        Prefs.putString(PREF_USER_ID, userId);
    }

    public static String getGcmToken() {
        return Prefs.getString(PREF_GCM_TOKEN, null);
    }

    public static void setGcmToken(String gcmToken) {
        Prefs.putString(PREF_GCM_TOKEN, gcmToken);
    }

    public static int getGcmTokenVersion() {
        return Prefs.getInt(PREF_GCM_TOKEN_VERSION, -1);
    }

    public static void setGcmTokenVersion(int gcmTokenVersion) {
        Prefs.putInt(PREF_GCM_TOKEN_VERSION, gcmTokenVersion);
    }

    public static void putReferrer(Referrer referrer) {
        Prefs.putString(PREF_REFERRER, referrer.getReferrerString());
    }

    public static String getReferrer() {
        String referrer = Prefs.getString(PREF_REFERRER, null);
        if(referrer != null) {
            Prefs.edit().remove(PREF_REFERRER).apply();
        }
        return referrer;
    }

    public static void putKillswitch(JSONObject killswitch) {
        Prefs.putString(PREF_KILLSWITCH, killswitch.toString());
    }

    public static KillswitchInfo getKillswitch() {
        try {
            JSONObject killswitch = new JSONObject(Prefs.getString(PREF_KILLSWITCH, "{}"));
            return new KillswitchInfo(app, killswitch);
        }
        catch(Exception e) {
            Logger.e("Couldn't create KillswitchInfo from prefs", e);
            return new KillswitchInfo(app, new JSONObject());
        }
    }

    public static boolean wasTopContactsShown() {
        return Prefs.getBoolean(PREF_SHOWED_TOP_CONTACTS, true);
    }

    public static void setTopContactsShown(boolean topContactsShown) {
        Prefs.putBoolean(PREF_SHOWED_TOP_CONTACTS, topContactsShown);
    }

    public static boolean wasFirstButtonPressed() {
        return Prefs.getBoolean(PREF_FIRST_BUTTON_PRESS, true);
    }

    public static void setFirstButtonPressed() {
        Prefs.putBoolean(PREF_FIRST_BUTTON_PRESS, true);
    }

    public static boolean shouldShowPreview() {
        return Prefs.getBoolean(PREF_SHOW_PREVIEW, false);
    }

    public static void setShouldShowPreview(boolean shouldShowPreview) {
        Prefs.putBoolean(PREF_SHOW_PREVIEW, shouldShowPreview);
    }

    public static DeliveryMethod getDeliveryMethod() {
        int method = Prefs.getInt(PREF_DELIVERY_METHOD, DEFAULT_DELIVERY_METHOD);
        if(method == 0) {
            return DeliveryMethod.SMS;
        }
        else if(method == 1 || method == -1) {
            return DeliveryMethod.CWSMS;
        }
        else if(method == 2) {
            return DeliveryMethod.EMAIL;
        }
        else if(method == 3) {
            return DeliveryMethod.FB;
        }
        else {
            return DeliveryMethod.TOP_CONTACTS;
        }
    }

    public static void setDeliveryMethod(DeliveryMethod method) {
        Prefs.putInt(PREF_DELIVERY_METHOD, method.getMethod());
    }

    public static long getMessageRefreshInterval() {
        return Prefs.getLong(PREF_MESSAGE_REFRESH_INTERVAL, DEFAULT_MESSAGE_REFRESH_INTERVAL);
    }

    public static void setMessageRefreshInterval(long messageRefreshInterval) {
        Prefs.putLong(PREF_MESSAGE_REFRESH_INTERVAL, messageRefreshInterval);
    }

    public static int getMaxDiskSpace() {
        return Prefs.getInt(PREF_MAX_DISK_SPACE, DEFAULT_MAX_DISK_SPACE);
    }

    public static void setMaxDiskSpace(int maxDiskSpace) {
        Prefs.putInt(PREF_MAX_DISK_SPACE, maxDiskSpace);
    }

    private static class Prefs {
        private static SharedPreferences prefs;

        private static SharedPreferences get() {
            if(prefs == null) {
                prefs = PreferenceManager.getDefaultSharedPreferences(app);
            }
            return prefs;
        }

        public static SharedPreferences.Editor edit() {
            return get().edit();
        }

        public static String getString(String key, String failValue) {
            return get().getString(key, failValue);
        }

        public static void putString(String key, String value) {
            get().edit().putString(key, value).apply();
        }

        public static int getInt(String key, int failValue) {
            return get().getInt(key, failValue);
        }

        public static void putInt(String key, int value) {
            get().edit().putInt(key, value).apply();
        }

        public static long getLong(String key, long failValue) {
            return get().getLong(key, failValue);
        }

        public static void putLong(String key, long value) {
            get().edit().putLong(key, value).apply();
        }

        public static float getFloat(String key, float failValue) {
            return get().getFloat(key, failValue);
        }

        public static void putFloat(String key, float value) {
            get().edit().putFloat(key, value).apply();
        }

        public static boolean getBoolean(String key, boolean failValue) {
            return get().getBoolean(key, failValue);
        }

        public static void putBoolean(String key, boolean value) {
            get().edit().putBoolean(key, value).apply();
        }

        public static boolean contains(String key) {
            return get().contains(key);
        }

        public static void remove(String key) {
            get().edit().remove(key).apply();
        }
    }

    //this method should only be called on initial migrate to android 2.0
    public static void migrateTo20() {
        //use constants here because keys might've changed
        String userId = Prefs.getString("USER_ID", null);
        boolean firstButtonPressed = Prefs.getBoolean("FIRST_BUTTON_PRESS", true);
        boolean showPreview = Prefs.getBoolean("PREF_SHOW_PREVIEW", false);
        int deliveryMethod = Prefs.getInt("PREF_DELIVERY_METHOD", -1);

        SharedPreferences.Editor e = Prefs.edit();
        e.clear() //clear old prefs
         .putBoolean(PREF_IS_FIRST_OPEN, false)
         .putString(PREF_USER_ID, userId)
         .putBoolean(PREF_FIRST_BUTTON_PRESS, firstButtonPressed)
         .putBoolean(PREF_SHOW_PREVIEW, showPreview)
         .putInt(PREF_DELIVERY_METHOD, deliveryMethod)
         .putBoolean(PREF_SHOWED_TOP_CONTACTS, true)
         .apply();
    }
}
