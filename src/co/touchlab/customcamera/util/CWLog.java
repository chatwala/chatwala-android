package co.touchlab.customcamera.util;

import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/14/13
 * Time: 1:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class CWLog
{

    public static final String CHAT_WALA = "ChatWala";

    public static void i(String s)
    {
        Log.i(CHAT_WALA, s);
    }

    public static void i(String s, Exception e)
    {
        Log.i(CHAT_WALA, s, e);
    }
}
