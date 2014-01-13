package com.chatwala.android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.ClearStoreCommand;
import com.chatwala.android.util.CameraUtils;
import com.chatwala.android.util.MessageDataStore;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/26/13
 * Time: 12:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class AppPrefs
{
    private static AppPrefs INSTANCE;
    private Application application;

    public static final String PREF_FIRST_OPEN = "FIRST_OPEN";
    public static final String PREF_USER_ID = "USER_ID";
    public static final String PREF_IMAGE_REVIEWED = "IMAGE_REVIEWED";
    public static final String PREF_SELECTED_EMAIL = "PREF_SELECTED_EMAIL";
    public static final String PREF_BIT_DEPTH = "PREF_BIT_DEPTH";
    public static final String PREF_CHECKED_HAPPY = "PREF_CHECKED_HAPPY";
    public static final String PREF_SHARE_COUNT = "PREF_SHARE_COUNT";
    public static final String PREF_USE_SMS = "PREF_USE_SMS";
    public static final String PREF_MESSAGE_LOAD_INTERVAL = "PREF_MESSAGE_LOAD_INTERVAL";
    public static final String PREF_DISK_SPACE_MAX = "PREF_DISK_SPACE_MAX";

    public static synchronized AppPrefs getInstance(Context context)
    {
        if (INSTANCE == null)
        {
            INSTANCE = new AppPrefs(context);
        }
        return INSTANCE;
    }

    private final SharedPreferences mSp;

    private AppPrefs(Context context)
    {
        mSp = PreferenceManager.getDefaultSharedPreferences(context);
        application = (Application) context.getApplicationContext();
    }

    /*public SharedPreferences getPrefs()
    {
        return mSp;
    }*/

    public Boolean isFirstOpen()
    {
        Boolean firstOpen = mSp.getBoolean(PREF_FIRST_OPEN, true);
        mSp.edit().putBoolean(PREF_FIRST_OPEN, false).apply();
        return firstOpen;
    }

    public void setUserId(String userId)
    {
        mSp.edit().putString(PREF_USER_ID, userId).apply();
    }

    public String getUserId()
    {
        return mSp.getString(PREF_USER_ID, null);
    }

    public void setImageReviewed()
    {
        mSp.edit().putBoolean(PREF_IMAGE_REVIEWED, true).apply();
    }

    public boolean isImageReviewed()
    {
        return mSp.getBoolean(PREF_IMAGE_REVIEWED, false);
    }

    public String getPrefSelectedEmail()
    {
        return mSp.getString(PREF_SELECTED_EMAIL, null);
    }

    public void setPrefSelectedEmail(String email)
    {
        mSp.edit().putString(PREF_SELECTED_EMAIL, email).apply();
    }

    public int getPrefBitDepth()
    {
        return mSp.getInt(PREF_BIT_DEPTH, CameraUtils.findVideoBitDepth(application));
    }

    public void setPrefBitDepth(int bitDepth)
    {
        mSp.edit().putInt(PREF_BIT_DEPTH, bitDepth).apply();
    }

    public boolean getPrefCheckedHappy()
    {
        return mSp.getBoolean(PREF_CHECKED_HAPPY, false);
    }

    public void setPrefCheckedHappy(boolean bitDepth)
    {
        mSp.edit().putBoolean(PREF_CHECKED_HAPPY, bitDepth).apply();
    }

    public int getPrefShareCount()
    {
        return mSp.getInt(PREF_SHARE_COUNT, 0);
    }

    public void setPrefShareCount(int bitDepth)
    {
        mSp.edit().putInt(PREF_SHARE_COUNT, bitDepth).apply();
    }

    public void setPrefUseSms(boolean useSms)
    {
        mSp.edit().putBoolean(PREF_USE_SMS, useSms).apply();
    }

    public boolean getPrefUseSms()
    {
        return mSp.getBoolean(PREF_USE_SMS, true);
    }

    public void setPrefMessageLoadInterval(int minutes)
    {
        mSp.edit().putInt(PREF_MESSAGE_LOAD_INTERVAL, minutes).apply();
        FetchMessagesService.init(application, minutes);
    }

    public int getPrefMessageLoadInterval()
    {
        return mSp.getInt(PREF_MESSAGE_LOAD_INTERVAL, 120);
    }

    public void setPrefDiskSpaceMax(int max)
    {
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

    public int getPrefDiskSpaceMax()
    {
        return mSp.getInt(PREF_DISK_SPACE_MAX, 500);
    }
}
