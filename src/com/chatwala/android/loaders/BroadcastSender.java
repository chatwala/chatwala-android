package com.chatwala.android.loaders;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 5:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class BroadcastSender {
    public static final String NEW_MESSAGES_BROADCAST = "NEW_MESSAGES_BROADCAST";

    public static void makeNewMessagesBroadcast(Context context) {
        sendBroadcast(context, NEW_MESSAGES_BROADCAST);
    }

    private static void sendBroadcast(Context context, String broadcast) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(broadcast));
    }
}
