package com.chatwala.android.util;

import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.app.ChatwalaApplication;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by Eliezer on 5/7/2014.
 */
public class CwAnalytics {
    private static String CATEGORY_FIRST_OPEN = "FIRST_OPEN";
    private static String CATEGORY_CONVERSATION_STARTER = "CONVERSATION_STARTER";
    private static String CATEGORY_CONVERSATION_REPLIER = "CONVERSATION_REPLIER";
    private static String CATEGORY_INVALID = "INVALID";

    private static String ACTION_REFERRER_RECEIVED = "REFERRER_RECEIVED";

    private static String ACTION_APP_OPEN = "APP_OPEN";
    private static String ACTION_APP_BACKGROUND = "APP_BACKGROUND";

    private static String ACTION_DRAWER_OPENED= "ACTION_DRAWER_OPENED";
    private static String ACTION_DRAWER_CLOSED= "ACTION_DRAWER_CLOSED";

    private static String ACTION_TOP_CONTACTS_LOADED = "TOP_CONTACTS_LOADED";
    private static String ACTION_TAP_NEXT = "TAP_NEXT";
    private static String ACTION_TAP_SKIP = "TAP_SKIP";
    private static String ACTION_TOP_CONTACTS_SENT = "TOP_CONTACTS_SENT";

    public static String ACTION_START_RECORDING = "START_RECORDING";
    public static String ACTION_CANCEL_RECORDING = "CANCEL_RECORDING";
    public static String ACTION_COMPLETE_RECORDING = "COMPLETE_RECORDING";

    public static String ACTION_START_REVIEW = "START_REVIEW";
    public static String ACTION_CANCEL_REVIEW = "CANCEL_REVIEW";
    public static String ACTION_COMPLETE_REVIEW = "COMPLETE_REVIEW";

    public static String ACTION_START_REACTION = "START_REACTION";
    public static String ACTION_CANCEL_REACTION = "CANCEL_REACTION";
    public static String ACTION_COMPLETE_REACTION = "COMPLETE_REACTION";

    private static String ACTION_BACKGROUND_WHILE_RECORDING = "BACKGROUND_WHILE_RECORDING";

    private static String ACTION_UPSELL_SHOWN_EVENT = "UPSELL_SHOWN";
    private static String ACTION_UPSELL_ADDED = "UPSELL_ADDED";
    private static String ACTION_UPSELL_CANCELED = "UPSELL_CANCELED";

    private static String ACTION_NUMBER_ADDED = "NUMBER_ADDED";
    private static String ACTION_RECIPIENT_ADDED = "RECIPIENT_ADDED";
    private static String ACTION_RECENT_ADDED = "RECENT_ADDED";
    private static String ACTION_MESSAGE_SEND_CANCELED = "MESSAGE_SEND_CANCELED";
    private static String ACTION_MESSAGE_SENT = "MESSAGE_SENT";
    private static String ACTION_MESSAGE_SENT_CONFIRMED = "MESSAGE_SENT_CONFIRMED";
    private static String ACTION_MESSAGE_SENT_RETRY = "MESSAGE_SENT_RETRY";
    private static String ACTION_MESSAGE_SENT_FAILED = "MESSAGE_SENT_FAILED";
    private static String ACTION_BACKGROUND_WHILE_SMS = "BACKGROUND_WHILE_SMS";

    private static String LABEL_TAP_BUTTON = "TAP_BUTTON";
    private static String LABEL_TAP_SCREEN = "TAP_SCREEN";
    private static String LABEL_AUTO_START = "AUTO_START";
    private static String LABEL_NO_TAP = "NO_TAP";

    private ChatwalaApplication app;
    private Tracker tracker;
    private AnalyticsCategoryType categoryType = AnalyticsCategoryType.CONVERSATION_STARTER;

    public enum AnalyticsCategoryType {
        NON_COVERSATION, CONVERSATION_STARTER, CONVERSATION_REPLIER, CONVERSATION_VIEWER
    }

    public enum Initiator {
        NULL, NONE, BUTTON, SCREEN, BACK, AUTO, ENVIRONMENT
    }

    private CwAnalytics() {}

    private static class Singleton {
        public static final CwAnalytics instance = new CwAnalytics();
    }

    private static CwAnalytics me() {
        return Singleton.instance;
    }

    public static void attachToApp(ChatwalaApplication app, Tracker tracker) {
        me().app = app;
        me().tracker = tracker;
    }

    public static void setAnalyticsCategory(AnalyticsCategoryType categoryType) {
        me().categoryType = categoryType;
    }

    private ChatwalaApplication getApp() {
        return app;
    }

    private AnalyticsCategoryType getCategoryType() {
        return categoryType;
    }

    public static void sendScreenHit(String screen) {
        me().tracker.setScreenName(screen);
        me().tracker.send(new HitBuilders.AppViewBuilder().build());
    }

    public static String getCategory() {
        if(me().getApp().isFirstOpen()) {
            return CATEGORY_FIRST_OPEN;
        }
        else {
            return me().getCategoryType().name();
        }
    }

    public static void sendReferrerReceivedEvent(Referrer referrer) {
        if(referrer != null && referrer.isValid()) {
            me().sendEvent(ACTION_REFERRER_RECEIVED, referrer.getReferrerString(), null);
        }
    }

    public static void sendAppOpenEvent() {
        me().sendEvent(ACTION_APP_OPEN, Initiator.NULL, null);
    }

    public static void sendAppBackgroundEvent() {
        me().sendEvent(ACTION_APP_BACKGROUND, Initiator.NULL, null);
    }

    public static void sendTopContactsLoadedEvent(int numContacts) {
        me().sendEvent(ACTION_TOP_CONTACTS_LOADED, Initiator.NULL, (long) numContacts);
    }

    public static void sendTapNextEvent(Initiator action, int numContacts) {
        me().sendEvent(ACTION_TAP_NEXT, action, (long) numContacts);
    }

    public static void sendTapSkipEvent() {
        me().sendEvent(ACTION_TAP_SKIP, Initiator.BUTTON, null);
    }

    public static void sendTopContactsSentEvent(int numContacts) {
        me().sendEvent(ACTION_TOP_CONTACTS_SENT, Initiator.NULL, (long) numContacts);
    }

    public static void sendDrawerOpened() {
        me().sendEvent(ACTION_DRAWER_OPENED, Initiator.NULL, null);
    }

    public static void sendDrawerClosed() {
        me().sendEvent(ACTION_DRAWER_CLOSED, Initiator.NULL, null);
    }

    public static void sendRecordingStartEvent(Initiator initiator, String action) {
        me().sendEvent(action, initiator, null);
    }

    public static void sendRecordingStopEvent(Initiator initiator, String action, long recordingLength) {
        me().sendEvent(action, initiator, recordingLength);
    }

    public static void sendBackgroundWhileRecordingEvent(Initiator initiator, long duration) {
        me().sendEvent(ACTION_BACKGROUND_WHILE_RECORDING, initiator, duration);
    }

    public static void sendReviewStartedEvent(Initiator initiator) {
        me().sendEvent(ACTION_START_REVIEW, initiator, null);
    }

    public static void sendReviewCancelledEvent(Initiator initiator, long duration) {
        me().sendEvent(ACTION_CANCEL_REVIEW, initiator, duration);
    }

    public static void sendReviewCompletedEvent(Initiator initiator, long duration) {
        me().sendEvent(ACTION_COMPLETE_REVIEW, initiator, duration);
    }

    public static void sendUpsellShownEvent() {
        me().sendEvent(ACTION_UPSELL_SHOWN_EVENT, Initiator.NULL, null);
    }

    public static void sendUpsellAddedEvent() {
        me().sendEvent(ACTION_UPSELL_ADDED, Initiator.SCREEN, null);
    }

    public static void sendUpsellCanceledEvent() {
        me().sendEvent(ACTION_UPSELL_CANCELED, Initiator.SCREEN, null);
    }

    public static void sendNumberAddedEvent() {
        me().sendEvent(ACTION_NUMBER_ADDED, Initiator.SCREEN, null);
    }

    public static void sendRecipientAddedEvent() {
        me().sendEvent(ACTION_RECIPIENT_ADDED, Initiator.SCREEN, null);
    }

    public static void sendRecentAddedEvent() {
        me().sendEvent(ACTION_RECENT_ADDED, Initiator.SCREEN, null);
    }

    public static void sendBackgroundWhileSmsEvent() {
        me().sendEvent(ACTION_BACKGROUND_WHILE_SMS, Initiator.NULL, null);
    }

    public static void sendMessageSendCanceledEvent() {
        me().sendEvent(ACTION_MESSAGE_SEND_CANCELED, Initiator.SCREEN, null);
    }

    public static void sendMessageSentEvent(String category) {
        me().sendSmsEvent(category, ACTION_MESSAGE_SENT, Initiator.BUTTON, null);
    }

    public static void sendMessageSentConfirmedEvent(String category) {
        me().sendSmsEvent(category, ACTION_MESSAGE_SENT_CONFIRMED, null, null);
    }

    public static void sendMessageSentRetryEvent(String category, int numRetries) {
        me().sendSmsEvent(category, ACTION_MESSAGE_SENT_RETRY, null, (long) numRetries);
    }

    public static void sendMessageSentFailedEvent(String category, boolean failedImmediately) {
        me().sendSmsEvent(category, ACTION_MESSAGE_SENT_FAILED, null, null);
    }

    private void sendEvent(String action, Initiator initiator, Long value) {
        sendEvent(action, initiatorToLabelString(initiator), value);
    }

    private void sendEvent(String action, String label, Long value) {
        label = label == null ? "" : label;
        value = value != null ? value : 0;
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("")
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());
        Logger.i("Sending Analytics event (tracking id is " + EnvironmentVariables.get().getGoogleAnalyticsId() + "):" +
                "\n\tCATEGORY:\t" + getCategory() +
                "\n\tACTION:\t" + action +
                "\n\tLABEL:\t" + label +
                "\n\tVALUE:\t" + value);
    }

    private void sendSmsEvent(String category, String action, Initiator initiator, Long value) {
        String categoryString = category != null ? category : "SMS_CATEGORY";
        String label = initiatorToLabelString(initiator);
        value = value != null ? value : 0;
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("")
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());
        Logger.i("Sending Analytics event (tracking id is " + EnvironmentVariables.get().getGoogleAnalyticsId() + "):" +
                "\n\tCATEGORY:\t" + categoryString +
                "\n\tACTION:\t" + action +
                "\n\tLABEL:\t" + label +
                "\n\tVALUE:\t" + value);
    }

    private String initiatorToLabelString(Initiator initiator) {
        if(initiator == null) {
            return "";
        }
        return initiator.name();
    }
}
