package co.touchlab.customcamera;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import co.touchlab.customcamera.util.CameraUtils;
import co.touchlab.customcamera.util.VideoUtils;

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

    public static final String PREF_SELECTED_EMAIL = "PREF_SELECTED_EMAIL";
    public static final String PREF_BIT_DEPTH = "PREF_BIT_DEPTH";
    public static final String PREF_CHECKED_HAPPY = "PREF_CHECKED_HAPPY";
    public static final String PREF_SHARE_COUNT = "PREF_SHARE_COUNT";


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
}
