package co.touchlab.customcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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

    public static final String PREF_SELECTED_EMAIL = "PREF_SELECTED_EMAIL";

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
}
