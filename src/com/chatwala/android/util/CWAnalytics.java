package com.chatwala.android.util;

import android.content.Context;
import android.util.Log;
import com.chatwala.android.AppPrefs;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

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

    private static Boolean isStarterMessage;
    private static String categoryString;
    private static EasyTracker easyTracker = null;

    public static void initAnalytics(Context context, boolean isStarter)
    {
        if(easyTracker == null)
        {
            easyTracker = EasyTracker.getInstance(context);
            isStarterMessage = isStarter;
            categoryString = (AppPrefs.getInstance(context).isFirstOpen() ? CATEGORY_FIRST_OPEN : (isStarter ? CATEGORY_CONVERSATION_STARTER : CATEGORY_CONVERSATION_REPLIER));
            sendAppOpenEvent();
        }
    }

    private static void sendAppOpenEvent()
    {
        sendEvent(ACTION_APP_OPEN, null, null);
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
        sendEvent(isStarterMessage ? ACTION_START_RECORDING : ACTION_START_REPLY, buttonTapped ? LABEL_TAB_BUTTON : LABEL_TAP_SCREEN, null);
    }

    public static void sendRecordingEndEvent(boolean buttonTapped, Long duration)
    {
        sendEvent(isStarterMessage ? ACTION_COMPLETE_RECORDING : ACTION_COMPLETE_REPLY, buttonTapped ? LABEL_TAB_BUTTON : LABEL_TAP_SCREEN, duration);
    }

    public static void sendSendMessageEvent(Long previewNum)
    {
        sendEvent(ACTION_SEND_MESSAGE, null, previewNum);
    }

    public static void sendRedoMessageEvent(Long previewNum)
    {
        sendEvent(ACTION_REDO_MESSAGE, null, previewNum);
    }

    private static void sendEvent(String action, String label, Long value)
    {
        String labelString = label != null ? label : "none";
        String valueString = value != null ? value.toString() : "none";
        Log.d("ANALYTICS #############", "CATEGORY: " + categoryString + " ACTION: " + action + " LABEL: " + labelString + " VALUE: " + valueString);
        easyTracker.send(MapBuilder.createEvent(categoryString, action, label, value).build());
    }
}
