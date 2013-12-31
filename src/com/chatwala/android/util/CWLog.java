package com.chatwala.android.util;

import android.util.Log;
import com.crashlytics.android.Crashlytics;

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

    public static final String USER_ACTION = "USER_ACTION";
    public static final String MEDIA_RECORDER_STATE = "MEDIA_RECORDER_STATE";

    private static final String PREVIEW_WIDTH = "PREVIEW_WIDTH";
    private static final String PREVIEW_HEIGHT = "PREVIEW_HEIGHT";
    private static final String VIDEO_WIDTH = "VIDEO_WIDTH";
    private static final String VIDEO_HEIGHT = "VIDEO_HEIGHT";
    private static final String FRAMERATE = "FRAMERATE";
    private static final String SHARE_LINK = "SHARE_LINK";

    public static void i(Class cl, String s)
    {
        Log.i(cl.getSimpleName(), s);
    }

    public static void i(Class cl, String s, Exception e)
    {
        Log.i(cl.getSimpleName(), s, e);
    }

    public static void userAction(Class cl, String s)
    {
        Log.d(cl.getSimpleName(), s);
        Crashlytics.log(Log.DEBUG, USER_ACTION, s);
    }

    public static void mediaRecorder(Class cl, String s)
    {
        Log.d(cl.getSimpleName(), s);
        Crashlytics.log(Log.DEBUG, MEDIA_RECORDER_STATE, s);
    }

    public static void b(Class cl, String s)
    {
        String simpleName = cl.getSimpleName();
        Log.w(simpleName, s);
        Crashlytics.log(simpleName + ": "+ s);
    }

    public static void softExceptionLog(Class cl, String s, Throwable t)
    {
        b(cl, s);
        Log.w(cl.getSimpleName(), s, t);
        Crashlytics.logException(t);
    }

    public static void setUserInfo(String userIdentifier, String userName, String userEmail)
    {
        Crashlytics.setUserIdentifier(userIdentifier);
        Crashlytics.setUserName(userName);
        Crashlytics.setUserEmail(userEmail);
    }

    public static void logPreviewDimensions(int width, int height)
    {
        Crashlytics.setInt(PREVIEW_WIDTH, width);
        Crashlytics.setInt(PREVIEW_HEIGHT, height);
    }

    public static void logVideoDimensions(int width, int height)
    {
        Crashlytics.setInt(VIDEO_WIDTH, width);
        Crashlytics.setInt(VIDEO_HEIGHT, height);
    }

    public static void logFramerate(int framerate)
    {
        Crashlytics.setInt(FRAMERATE, framerate);
    }

    public static void logShareLink(String link)
    {
        Crashlytics.setString(SHARE_LINK, link);
    }
}
