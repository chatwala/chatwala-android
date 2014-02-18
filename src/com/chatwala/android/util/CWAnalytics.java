package com.chatwala.android.util;

import android.content.Context;
import android.util.Log;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.http.BaseHttpRequest;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/12/13
 * Time: 9:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class CWAnalytics
{
    private static String CATEGORY_FIRST_OPEN = "FIRST_OPEN";
    private static String CATEGORY_CONVERSATION_STARTER = "CONVERSATION_STARTER";
    private static String CATEGORY_CONVERSATION_REPLIER = "CONVERSATION_REPLIER";

    private static String ACTION_APP_OPEN = "APP_OPEN";
    private static String ACTION_APP_BACKGROUND = "APP_BACKGROUND";
    private static String ACTION_START_RECORDING = "START_RECORDING";
    private static String ACTION_COMPLETE_RECORDING = "COMPLETE_RECORDING";
    private static String ACTION_SEND_MESSAGE = "SEND_MESSAGE";
    private static String ACTION_REDO_MESSAGE = "REDO_MESSAGE";
    private static String ACTION_SEND_EMAIL = "SEND_EMAIL";
    private static String ACTION_CANCEL_EMAIL = "CANCEL_EMAIL";
    private static String ACTION_START_REVIEW = "START_REVIEW";
    private static String ACTION_STOP_REVIEW = "STOP_REVIEW";
    private static String ACTION_COMPLETE_REVIEW = "COMPLETE_REVIEW";
    private static String ACTION_START_REACTION = "START_REACTION";
    private static String ACTION_STOP_REACTION = "STOP_REACTION";
    private static String ACTION_COMPLETE_REACTION = "COMPLETE_REACTION";
    private static String ACTION_START_REPLY = "START_REPLY";
    private static String ACTION_COMPLETE_REPLY = "COMPLETE_REPLY";

    private static String LABEL_TAB_BUTTON = "TAP_BUTTON";
    private static String LABEL_TAP_SCREEN = "TAP_SCREEN";
    private static String LABEL_NO_TAP = "NO_TAP";

    private static Boolean isStarterMessage= false;
    private static String categoryString = null;
    //private static EasyTracker easyTracker = null;
    private static Tracker tracker = null;

    private static Context context;
    private static Boolean isFirstOpen=false;

    private static int numRedos =0;

    public static void initAnalytics(Context ctx)
    {
        context = ctx;
        isFirstOpen = AppPrefs.getInstance(context).isFirstOpen();
        if(tracker == null) {
            tracker = GoogleAnalytics.getInstance(context).getTracker(BaseHttpRequest.getApiInfo().getGoogleAnalyticsID());
        }
        calculateCategory();
    }

    public static void setStarterMessage(Boolean isStarter) {

       isStarterMessage = isStarter;

       //reset redos
       if(!isStarter) {
           Log.d("ANALYTICS #############","Reset redos");
           resetRedos();
       }
       calculateCategory();
    }

    public static void resetRedos() {
        numRedos=0;
    }

    public static void calculateCategory() {
        categoryString = (isFirstOpen ? CATEGORY_FIRST_OPEN : (isStarterMessage ? CATEGORY_CONVERSATION_STARTER : CATEGORY_CONVERSATION_REPLIER));
    }

    public static void sendAppOpenEvent()
    {
        sendEvent(ACTION_APP_OPEN, null, null);
    }

    public static void sendAppBackgroundEvent()
    {
        isFirstOpen = false;
        sendEvent(ACTION_APP_BACKGROUND, null, null);
        calculateCategory();
    }

    public static void sendStartReviewEvent()
    {
        sendEvent(ACTION_START_REVIEW, null, null);
    }

    public static void sendStopReviewEvent(Long duration)
    {
        sendEvent(ACTION_STOP_REVIEW, null, duration);
    }

    public static void sendReviewCompleteEvent(Long duration)
    {
        sendEvent(ACTION_COMPLETE_REVIEW, null, duration);
    }

    public static void sendStartReactionEvent()
    {
        sendEvent(ACTION_START_REACTION, null, null);
    }

    public static void sendStopReactionEvent(Long duration)
    {
        sendEvent(ACTION_STOP_REACTION, null, duration);
    }

    public static void sendReactionCompleteEvent(Long duration)
    {
        sendEvent(ACTION_COMPLETE_REACTION, null, duration);
    }

    public static void sendRecordingStartEvent(boolean buttonTapped)
    {
        sendEvent(isStarterMessage ? ACTION_START_RECORDING : ACTION_START_REPLY, buttonTapped ? LABEL_TAB_BUTTON : LABEL_NO_TAP, null);
    }

    public static void sendRecordingEndEvent(boolean buttonTapped, Long duration)
    {
        sendEvent(isStarterMessage ? ACTION_COMPLETE_RECORDING : ACTION_COMPLETE_REPLY, buttonTapped ? LABEL_TAB_BUTTON : LABEL_NO_TAP, duration);
    }

    public static void sendSendMessageEvent(Long previewNum)
    {
        sendEvent(ACTION_SEND_MESSAGE, null, new Long(numRedos));
        resetRedos();
    }

    public static void sendRedoMessageEvent(Long previewNum)
    {
        numRedos++;
        sendEvent(ACTION_REDO_MESSAGE, null, new Long(numRedos));
    }

    private static void sendEvent(String action, String label, Long value)
    {
        String labelString = label != null ? label : "none";
        String valueString = value != null ? value.toString() : "none";
        Log.d("ANALYTICS #############", "CATEGORY: " + categoryString + " ACTION: " + action + " LABEL: " + labelString + " VALUE: " + valueString);
        tracker.send(MapBuilder.createEvent(categoryString, action, label, value).build());
    }
}
