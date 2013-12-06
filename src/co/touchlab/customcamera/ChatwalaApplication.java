package co.touchlab.customcamera;

import android.app.Application;
import android.graphics.Typeface;
import com.crashlytics.android.Crashlytics;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/5/13
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaApplication extends Application
{
    static final String FONT_DIR = "fonts/";
    private static final String ITCAG_DEMI = "ITCAvantGardeStd-Demi.otf",
                ITCAG_MD = "ITCAvantGardeStd-Md.otf";

    public Typeface fontMd;
    public Typeface fontDemi;

    private boolean splashRan;

    @Override
    public void onCreate()
    {
        super.onCreate();

        Crashlytics.start(this);

        fontMd = Typeface.createFromAsset(getAssets(), FONT_DIR + ITCAG_MD);
        fontDemi = Typeface.createFromAsset(getAssets(), FONT_DIR + ITCAG_DEMI);
    }

    public boolean isSplashRan()
    {
        return splashRan;
    }

    public void setSplashRan(boolean splashRan)
    {
        this.splashRan = splashRan;
    }
}
