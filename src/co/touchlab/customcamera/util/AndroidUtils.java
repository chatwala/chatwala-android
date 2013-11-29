package co.touchlab.customcamera.util;

import android.os.Looper;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/28/13
 * Time: 11:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class AndroidUtils
{
    public static boolean isMainThread()
    {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
