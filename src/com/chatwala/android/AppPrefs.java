package com.chatwala.android;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.activity.SettingsActivity.DeliveryMethod;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.ClearStoreCommand;
import com.chatwala.android.util.CameraUtils;
import com.chatwala.android.util.KillswitchInfo;
import com.chatwala.android.util.Logger;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/26/13
 * Time: 12:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class AppPrefs {
    private static AppPrefs INSTANCE;
    private Application application;

    public static final String PREF_KILLSWITCH = "KILLSWITCH";
    public static final String PREF_REFERRER = "PREF_REFERRER";
    public static final String PREF_FIRST_OPEN = "FIRST_OPEN";
    public static final String PREF_FIRST_BUTTON_PRESS = "FIRST_BUTTON_PRESS";
    public static final String PREF_USER_ID = "USER_ID";
    public static final String PREF_GCM_TOKEN = "GCM_TOKEN";
    public static final String PREF_GCM_APP_VERSION = "GCM_APP_VERSION";
    public static final String PREF_IMAGE_REVIEWED = "IMAGE_REVIEWED";
    public static final String PREF_SELECTED_EMAIL = "PREF_SELECTED_EMAIL";
    public static final String PREF_SHOW_PREVIEW = "PREF_SHOW_PREVIEW";
    public static final String PREF_BIT_DEPTH = "PREF_BIT_DEPTH";
    public static final String PREF_CHECKED_HAPPY = "PREF_CHECKED_HAPPY";
    public static final String PREF_SHARE_COUNT = "PREF_SHARE_COUNT";
    public static final String PREF_USE_SMS = "PREF_USE_SMS";
    public static final String PREF_DELIVERY_METHOD = "PREF_DELIVERY_METHOD";
    public static final String PREF_MESSAGE_LOAD_INTERVAL = "PREF_MESSAGE_LOAD_INTERVAL";
    public static final String PREF_DISK_SPACE_MAX = "PREF_DISK_SPACE_MAX";
    public static final String PREF_FEEDBACK_SHOWN = "PREF_FEEDBACK_SHOWN";
    public static final String PREF_FEEDBACK_MESSAGE_COUNT = "PREF_FEEDBACK_MESSAGE_COUNT";
    public static final String PREF_ACTION_INCREMENT = "PREF_ACTION_INCREMENT";

    public static final String PREF_SHOWED_TOP_CONTACTS = "SHOWED_TOP_CONTACT";

    public static synchronized AppPrefs getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AppPrefs(context);
        }
        return INSTANCE;
    }

    private final SharedPreferences mSp;

    private AppPrefs(Context context) {
        mSp = PreferenceManager.getDefaultSharedPreferences(context);
        application = (Application) context.getApplicationContext();
    }

    /*public SharedPreferences getPrefs()
    {
        return mSp;
    }*/

    public void putKillswitch(JSONObject killswitch) {
        mSp.edit().putString(PREF_KILLSWITCH, killswitch.toString()).apply();
    }

    public KillswitchInfo getKillswitch() {
        try {
            JSONObject killswitch = new JSONObject(mSp.getString(PREF_KILLSWITCH, "{}"));
            return new KillswitchInfo(application, killswitch);
        }
        catch(Exception e) {
            Logger.e("Couldn't create KillswitchInfo from prefs", e);
            return new KillswitchInfo(application, new JSONObject());
        }
    }

    public void putReferrer(String referrer) {
        mSp.edit().putString(PREF_REFERRER, referrer).apply();
    }

    public String getReferrer() {
        String referrer = mSp.getString(PREF_REFERRER, null);
        if(referrer != null) {
            mSp.edit().remove(PREF_REFERRER).apply();
        }
        return referrer;
    }

    public int getActionIncrement() {
        int actionIncrement = mSp.getInt(PREF_ACTION_INCREMENT, 0);
        mSp.edit().putInt(PREF_ACTION_INCREMENT, 0).apply();
        return actionIncrement;
    }

    public void setActionIncrement(int actionIncrement) {
        mSp.edit().putInt(PREF_ACTION_INCREMENT, actionIncrement).apply();
    }

    public Boolean isFirstOpen() {
        Boolean firstOpen = mSp.getBoolean(PREF_FIRST_OPEN, true);
        if(firstOpen) {
            mSp.edit()
                    .putBoolean(PREF_FIRST_BUTTON_PRESS, true)
                    .putBoolean(PREF_SHOWED_TOP_CONTACTS, true).apply();
            setDeliveryMethod(DeliveryMethod.CWSMS);
        }
        else {
            if(mSp.contains(PREF_USE_SMS)) { //if coming from 1.4.6 or before //TODO take out after killswitch thrown
                if(mSp.getBoolean(PREF_USE_SMS, true)) {
                    setDeliveryMethod(DeliveryMethod.CWSMS); //and they use SMS, switch them to CWSMS
                }
                else {
                    setDeliveryMethod(DeliveryMethod.EMAIL);
                }
                mSp.edit().remove(PREF_USE_SMS).apply();
            }
            else {
                UpdateManager.handleUpdateIfNeeded(application);
            }
        }
        mSp.edit().putBoolean(PREF_FIRST_OPEN, false).apply();
        return firstOpen;
    }

    public boolean wasTopContactsShown() {
        return mSp.getBoolean(PREF_SHOWED_TOP_CONTACTS, true);
    }

    public void setTopContactsShown(boolean topContactsShown) {
        mSp.edit().putBoolean(PREF_SHOWED_TOP_CONTACTS, topContactsShown).apply();
    }

    public boolean wasFirstButtonPressed() {
        return mSp.getBoolean(PREF_FIRST_BUTTON_PRESS, true);
    }

    public void setFirstButtonPressed() {
        mSp.edit().putBoolean(PREF_FIRST_BUTTON_PRESS, true).apply();
    }

    public void setUserId(String userId) {
        mSp.edit().putString(PREF_USER_ID, userId).apply();
    }

    public String getUserId() {
        return mSp.getString(PREF_USER_ID, null);
    }

    public void setGcmToken(String token) {
        mSp.edit().putString(PREF_GCM_TOKEN, token).apply();
    }

    public String getGcmToken() {
        return mSp.getString(PREF_GCM_TOKEN, null);
    }

    public void setGcmAppVersion(Integer version) {
        mSp.edit().putInt(PREF_GCM_APP_VERSION, version).apply();
    }

    public Integer getGcmAppVersion() {
        return mSp.getInt(PREF_GCM_APP_VERSION, 0);
    }

    public void setImageReviewed() {
        mSp.edit().putBoolean(PREF_IMAGE_REVIEWED, true).apply();
    }

    public boolean isImageReviewed() {
        return mSp.getBoolean(PREF_IMAGE_REVIEWED, true);
    }

    public String getPrefSelectedEmail() {
        return mSp.getString(PREF_SELECTED_EMAIL, null);
    }

    public void setPrefSelectedEmail(String email) {
        mSp.edit().putString(PREF_SELECTED_EMAIL, email).apply();
    }

    public boolean getPrefShowPreview() {
        return mSp.getBoolean(PREF_SHOW_PREVIEW, false);
    }

    public void setPrefShowPreview(boolean showPreview) {
        mSp.edit().putBoolean(PREF_SHOW_PREVIEW, showPreview).apply();
    }

    public int getPrefBitDepth() {
        return mSp.getInt(PREF_BIT_DEPTH, CameraUtils.findVideoBitDepth(application));
    }

    public void setPrefBitDepth(int bitDepth) {
        mSp.edit().putInt(PREF_BIT_DEPTH, bitDepth).apply();
    }

    public boolean getPrefCheckedHappy() {
        return mSp.getBoolean(PREF_CHECKED_HAPPY, false);
    }

    public void setPrefCheckedHappy(boolean bitDepth) {
        mSp.edit().putBoolean(PREF_CHECKED_HAPPY, bitDepth).apply();
    }

    public int getPrefShareCount() {
        return mSp.getInt(PREF_SHARE_COUNT, 0);
    }

    public void setPrefShareCount(int bitDepth) {
        mSp.edit().putInt(PREF_SHARE_COUNT, bitDepth).apply();
    }

    public void setDeliveryMethod(DeliveryMethod method) {
        mSp.edit().putInt(PREF_DELIVERY_METHOD, method.getMethod()).apply();
    }

    public DeliveryMethod getDeliveryMethod() {
        int method = mSp.getInt(PREF_DELIVERY_METHOD, 1);
        if(method == 0) {
            return DeliveryMethod.SMS;
        }
        else if(method == 1) {
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

    public void setPrefMessageLoadInterval(int minutes) {
        mSp.edit().putInt(PREF_MESSAGE_LOAD_INTERVAL, minutes).apply();
        application.startService(new Intent(application, FetchMessagesService.class));
    }

    public int getPrefMessageLoadInterval() {
        return mSp.getInt(PREF_MESSAGE_LOAD_INTERVAL, 60);
    }

    public void setPrefDiskSpaceMax(int max) {
        mSp.edit().putInt(PREF_DISK_SPACE_MAX, max).apply();
        DataProcessor.runProcess(new Runnable()
        {
            @Override
            public void run()
            {
                BusHelper.submitCommandSync(application, new ClearStoreCommand());
            }
        });
    }

    public int getPrefDiskSpaceMax() {
        return mSp.getInt(PREF_DISK_SPACE_MAX, 500);
    }

    public void setPrefFeedbackShown(boolean shouldShowFeedback) {
        mSp.edit().putBoolean(PREF_FEEDBACK_SHOWN, shouldShowFeedback).apply();
    }

    public boolean getPrefFeedbackShown() {
        return mSp.getBoolean(PREF_FEEDBACK_SHOWN, false);
    }

    public boolean recordMessageSent() {
        int showCount = mSp.getInt(PREF_FEEDBACK_MESSAGE_COUNT, 0);
        showCount++;
        mSp.edit().putInt(PREF_FEEDBACK_MESSAGE_COUNT, showCount).apply();
        if(showCount >= 5) {
            setPrefFeedbackShown(true);
            return true;
        }
        else {
            return false;
        }
    }
}
