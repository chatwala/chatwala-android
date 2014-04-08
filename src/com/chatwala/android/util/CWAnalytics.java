package com.chatwala.android.util;

import android.content.Context;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.EnvironmentVariables;
import com.chatwala.android.activity.SettingsActivity.DeliveryMethod;
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
    private static String CATEGORY_REFERRER = "REFERRER";
    private static String CATEGORY_FIRST_OPEN = "FIRST_OPEN";
    private static String CATEGORY_CONVERSATION_STARTER = "CONVERSATION_STARTER";
    private static String CATEGORY_CONVERSATION_REPLIER = "CONVERSATION_REPLIER";

    private static String CATEGORY_FIRST_OPEN_FACEBOOK = "FIRST_OPEN_FACEBOOK";
    private static String CATEGORY_FIRST_OPEN_MESSAGE = "FIRST_OPEN_MESSAGE";
    private static String CATEGORY_FIRST_OPEN_COPY = "FIRST_OPEN_COPY";

    private static String CATEGORY_AD_REFERRER_FACEBOOK = "AD_REFERRER_FACEBOOK";

    private static String ACTION_REFERRER_RECEIVED = "REFERRER_RECEIVED";

    private static String ACTION_APP_OPEN = "APP_OPEN";
    private static String ACTION_APP_BACKGROUND = "APP_BACKGROUND";
    private static String ACTION_DRAWER_OPENED= "ACTION_DRAWER_OPENED";
    private static String ACTION_DRAWER_CLOSED= "ACTION_DRAWER_CLOSED";
    private static String ACTION_TOP_CONTACTS_LOADED = "TOP_CONTACTS_LOADED";
    private static String ACTION_TAP_NEXT = "TAP_NEXT";
    private static String ACTION_TOP_CONTACTS_SENT = "TOP_CONTACTS_SENT";
    private static String ACTION_START_RECORDING = "START_RECORDING";
    private static String ACTION_STOP_RECORDING = "STOP_RECORDING";
    private static String ACTION_BACKGROUND_WHILE_RECORDING = "BACKGROUND_WHILE_RECORDING";
    private static String ACTION_COMPLETE_RECORDING = "COMPLETE_RECORDING";
    private static String ACTION_SEND_MESSAGE = "SEND_MESSAGE";
    private static String ACTION_START_PREVIEW = "START_PREVIEW";
    private static String ACTION_REDO_MESSAGE = "REDO_MESSAGE";
    private static String ACTION_START_REVIEW = "START_REVIEW";
    private static String ACTION_STOP_REVIEW = "STOP_REVIEW";
    private static String ACTION_BACKGROUND_WHILE_REVIEW= "BACKGROUND_WHILE_REVIEW";
    private static String ACTION_COMPLETE_REVIEW = "COMPLETE_REVIEW";
    private static String ACTION_START_REACTION = "START_REACTION";
    private static String ACTION_STOP_REACTION = "STOP_REACTION";
    private static String ACTION_BACKGROUND_WHILE_REACTION = "BACKGROUND_WHILE_REACTION";
    private static String ACTION_COMPLETE_REACTION = "COMPLETE_REACTION";
    private static String ACTION_START_REPLY = "START_REPLY";
    private static String ACTION_STOP_REPLY = "STOP_REPLY";
    private static String ACTION_BACKGROUND_WHILE_REPLY = "BACKGROUND_WHILE_REPLY";
    private static String ACTION_COMPLETE_REPLY = "COMPLETE_REPLY";
    private static String ACTION_STOP_PRESSED = "STOP_PRESSED";
    private static String ACTION_BACK_PRESSED = "BACK_PRESSED";

    private static String ACTION_UPSELL_SHOWN_EVENT = "UPSELL_SHOWN";
    private static String ACTION_UPSELL_ADDED = "UPSELL_ADDED";
    private static String ACTION_UPSELL_CANCELED = "UPSELL_CANCELED";

    private static String ACTION_RECIPIENT_ADDED = "RECIPIENT_ADDED";
    private static String ACTION_RECENT_ADDED = "RECENT_ADDED";
    private static String ACTION_MESSAGE_SEND_CANCELED = "MESSAGE_SEND_CANCELED";
    private static String ACTION_MESSAGE_SENT = "MESSAGE_SENT";
    private static String ACTION_MESSAGE_SENT_CONFIRMED = "MESSAGE_SENT_CONFIRMED";
    private static String ACTION_MESSAGE_SENT_FAILED = "MESSAGE_SENT_FAILED";
    private static String ACTION_BACKGROUND_WHILE_SMS = "BACKGROUND_WHILE_SMS";

    private static String ACTION_FACEBOOK_SEND_CONFIRMED = "FACEBOOK_SEND_CONFIRMED";
    private static String ACTION_FACEBOOK_SEND_CANCELED = "FACEBOOK_SEND_CANCELED";

    private static String LABEL_TAP_BUTTON = "TAP_BUTTON";
    private static String LABEL_TAP_SCREEN = "TAP_SCREEN";
    private static String LABEL_AUTO_START = "AUTO_START";
    private static String LABEL_NO_TAP = "NO_TAP";

    private static String LABEL_FAILED_IMMEDIATELY = "FAILED_IMMEDIATELY";
    private static String LABEL_FAILED_RETRIES = "FAILED_RETRIES";

    private static Boolean isStarterMessage= false;
    private static String categoryString = null;
    //private static EasyTracker easyTracker = null;
    private static Tracker tracker = null;

    private static Context context;
    private static Boolean isFirstOpen=false;

    private static int numRedos =0;

    private static int actionIncrement=0;

    public static void initAnalytics(Context ctx)
    {
        context = ctx;
        isFirstOpen = AppPrefs.getInstance(context).isFirstOpen();
        actionIncrement = AppPrefs.getInstance(context).getActionIncrement();

        if(tracker == null) {
            tracker = GoogleAnalytics.getInstance(context).getTracker(EnvironmentVariables.get().getGoogleAnalyticsID());
        }
        calculateCategory(null);
    }

    public static void setStarterMessage(Boolean isStarter, Referrer referrer) {

       isStarterMessage = isStarter;

       //reset redos & action increment
       if(!isStarter) {
           resetRedos();
           resetActionIncrement();
       }
       calculateCategory(referrer);
    }

    private static void incrementAction() {
        actionIncrement++;
        AppPrefs.getInstance(context).setActionIncrement(actionIncrement);
    }

    private static void resetActionIncrement() {
        actionIncrement =0;
        AppPrefs.getInstance(context).setActionIncrement(0);
    }

    public static void resetRedos() {
        numRedos=0;
    }

    public static void calculateCategory(Referrer referrer) {
        String oldCategoryString = new String(categoryString==null?"":categoryString);
        categoryString = (isFirstOpen ? CATEGORY_FIRST_OPEN : (isStarterMessage ? CATEGORY_CONVERSATION_STARTER : CATEGORY_CONVERSATION_REPLIER));

        if(!categoryString.equals(oldCategoryString)) {
            resetActionIncrement();
        }
    }

    public static String getCategory() {
        return categoryString;
    }

    public static void sendReferrerReceivedEvent(Referrer referrer) {
        calculateCategory(referrer);
        if(referrer != null && referrer.isValid()) {
            sendEvent(ACTION_REFERRER_RECEIVED, referrer.getReferrerString(), null);
        }
    }

    public static void sendAppOpenEvent()
    {
        sendEvent(ACTION_APP_OPEN, null, null);
    }

    public static void sendAppBackgroundEvent()
    {
        isFirstOpen = false;
        sendEvent(ACTION_APP_BACKGROUND, null, new Long(actionIncrement));
        calculateCategory(null);
        incrementAction();
    }

    public static void sendTopContactsLoadedEvent(int numContacts) {
        sendEvent(ACTION_TOP_CONTACTS_LOADED, null, (long) numContacts);
    }

    public static void sendTapNextEvent(boolean buttonTapped, int numContacts) {
        sendEvent(ACTION_TAP_NEXT, (buttonTapped ? LABEL_TAP_BUTTON : LABEL_TAP_SCREEN), (long) numContacts);
    }

    public static void sendTopContactsSentEvent(int numContacts) {
        sendEvent(ACTION_TOP_CONTACTS_SENT, null, (long) numContacts);
    }

    public static void sendDrawerOpened() {
        sendEvent(ACTION_DRAWER_OPENED, null, new Long(actionIncrement));
        incrementAction();
    }

    public static void sendDrawerClosed() {
        sendEvent(ACTION_DRAWER_CLOSED, null, null);
    }

    public static void sendRecordingStartEvent(boolean buttonTapped, boolean autoStarted)
    {
        sendEvent(ACTION_START_RECORDING, (autoStarted? LABEL_AUTO_START : buttonTapped ? LABEL_TAP_BUTTON : LABEL_TAP_SCREEN), new Long(actionIncrement));
        incrementAction();
    }

    public static void sendRecordingStopEvent(long duration) {
        sendEvent(ACTION_STOP_RECORDING, LABEL_TAP_BUTTON, duration);
    }

    public static void sendBackgroundWhileRecordingEvent(long duration) {
        sendEvent(ACTION_BACKGROUND_WHILE_RECORDING, LABEL_NO_TAP, duration);
    }

    public static void sendRecordingCompleteEvent(Long duration)
    {
        sendEvent(ACTION_COMPLETE_RECORDING, LABEL_NO_TAP, duration);
    }

    public static void sendReactionStartEvent()
    {
        sendEvent(ACTION_START_REACTION, LABEL_NO_TAP, null);
    }

    public static void sendReactionStartEvent(boolean buttonTapped)
    {
        sendEvent(ACTION_START_REACTION, buttonTapped ? LABEL_TAP_BUTTON : LABEL_TAP_SCREEN, null);
    }

    public static void sendReactionStopEvent(Long duration)
    {
        sendEvent(ACTION_STOP_REACTION, LABEL_TAP_BUTTON, duration);
    }

    public static void sendBackgroundWhileReactionEvent(long duration) {
        sendEvent(ACTION_BACKGROUND_WHILE_REACTION, LABEL_NO_TAP, duration);
    }

    public static void sendReactionCompleteEvent(Long duration)
    {
        sendEvent(ACTION_COMPLETE_REACTION, LABEL_NO_TAP, duration);
    }

    public static void sendReplyStartEvent()
    {
        sendEvent(ACTION_START_REPLY, LABEL_NO_TAP, new Long(actionIncrement));
        incrementAction();
    }

    public static void sendReplyStopEvent(long duration) {
        sendEvent(ACTION_STOP_REPLY, LABEL_TAP_BUTTON, duration);
    }

    public static void sendBackgroundWhileReplyEvent(long duration) {
        sendEvent(ACTION_BACKGROUND_WHILE_REPLY, LABEL_NO_TAP, duration);
    }

    public static void sendReplyCompleteEvent(Long duration)
    {
        sendEvent(ACTION_COMPLETE_REPLY, LABEL_NO_TAP, duration);
    }

    public static void sendReviewStartEvent(boolean buttonTapped)
    {
        sendEvent(ACTION_START_REVIEW, buttonTapped ? LABEL_TAP_BUTTON : LABEL_TAP_SCREEN, null);
    }

    public static void sendReviewStopEvent(Long duration)
    {
        sendEvent(ACTION_STOP_REVIEW, LABEL_TAP_BUTTON, duration);
    }

    public static void sendBackgroundWhileReviewEvent(long duration) {
        sendEvent(ACTION_BACKGROUND_WHILE_REVIEW, LABEL_NO_TAP, duration);
    }

    public static void sendReviewCompleteEvent(Long duration)
    {
        sendEvent(ACTION_COMPLETE_REVIEW, LABEL_NO_TAP, duration);
    }

    public static void sendStopPressedEvent(long duration) {
        sendEvent(ACTION_STOP_PRESSED, LABEL_TAP_BUTTON, duration);
    }

    public static void sendSendMessageEvent(DeliveryMethod method, Long previewNum)
    {
        sendEvent(ACTION_SEND_MESSAGE, method.getDisplayString(), new Long(numRedos));
        resetRedos();
    }

    public static void sendPreviewStartEvent() {
        sendEvent(ACTION_START_PREVIEW, LABEL_NO_TAP, null);
    }

    public static void sendRedoMessageEvent(Long previewNum)
    {
        numRedos++;
        sendEvent(ACTION_REDO_MESSAGE, null, new Long(numRedos));
    }

    public static void sendUpsellShownEvent() {
        sendEvent(ACTION_UPSELL_SHOWN_EVENT, LABEL_NO_TAP, null);
    }

    public static void sendUpsellAddedEvent() {
        sendEvent(ACTION_UPSELL_ADDED, LABEL_TAP_SCREEN, null);
    }

    public static void sendUpsellCanceledEvent() {
        sendEvent(ACTION_UPSELL_CANCELED, LABEL_TAP_SCREEN, null);
    }

    public static void sendRecipientAddedEvent() {
        sendEvent(ACTION_RECIPIENT_ADDED, LABEL_TAP_SCREEN, null);
    }

    public static void sendRecentAddedEvent() {
        sendEvent(ACTION_RECENT_ADDED, LABEL_TAP_SCREEN, null);
    }

    public static void sendMessageSendCanceledEvent() {
        sendEvent(ACTION_MESSAGE_SEND_CANCELED, LABEL_TAP_SCREEN, null);
    }

    public static void sendMessageSentEvent(String category) {
        sendSmsEvent(category, ACTION_MESSAGE_SENT, LABEL_TAP_BUTTON, null);
    }

    public static void sendMessageSentConfirmedEvent(String category) {
        sendSmsEvent(category, ACTION_MESSAGE_SENT_CONFIRMED, LABEL_NO_TAP, null);
    }

    public static void sendMessageSentRetryEvent(String category, int numRetries) {
        sendSmsEvent(category, ACTION_MESSAGE_SENT_CONFIRMED, LABEL_NO_TAP, (long) numRetries);
    }

    public static void sendMessageSentFailedEvent(String category, boolean failedImmediately) {
        sendSmsEvent(category, ACTION_MESSAGE_SENT_FAILED, (failedImmediately ? LABEL_FAILED_IMMEDIATELY : LABEL_FAILED_RETRIES), null);
    }

    public static void sendFacebookSendConfirmed() {
        sendEvent(ACTION_FACEBOOK_SEND_CONFIRMED, LABEL_NO_TAP, null);
    }

    public static void sendFacebookSendCanceled() {
        sendEvent(ACTION_FACEBOOK_SEND_CANCELED, LABEL_NO_TAP, null);
    }

    public static void sendBackgroundWhileSmsEvent() {
        sendEvent(ACTION_BACKGROUND_WHILE_SMS, LABEL_NO_TAP, null);
    }

    public static void sendBackPressedEvent() {
        sendEvent(ACTION_BACK_PRESSED, LABEL_TAP_BUTTON, null);
    }

    private static void sendEvent(String action, String label, Long value) {
        String labelString = label != null ? label : LABEL_NO_TAP;
        String valueString = value != null ? value.toString() : "none";
        tracker.send(MapBuilder.createEvent(categoryString, action, label, value).build());
        Logger.i("Sending Analytics event (tracking id is " + EnvironmentVariables.get().getGoogleAnalyticsID() + "):" +
                "\n\tCATEGORY:\t" + categoryString +
                "\n\tACTION:\t" + action +
                "\n\tLABEL:\t" + labelString +
                "\n\tVALUE:\t" + valueString);
    }

    private static void sendSmsEvent(String category, String action, String label, Long value) {
        String categoryString = category != null ? category : "SMS_CATEGORY";
        String labelString = label != null ? label : LABEL_NO_TAP;
        String valueString = value != null ? value.toString() : "none";
        tracker.send(MapBuilder.createEvent(categoryString, action, label, value).build());
        Logger.i("Sending Analytics event (tracking id is " + EnvironmentVariables.get().getGoogleAnalyticsID() + "):" +
                "\n\tCATEGORY:\t" + categoryString +
                "\n\tACTION:\t" + action +
                "\n\tLABEL:\t" + labelString +
                "\n\tVALUE:\t" + valueString);
    }
}
