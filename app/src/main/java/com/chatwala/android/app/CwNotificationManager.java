package com.chatwala.android.app;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import com.chatwala.android.R;
import com.chatwala.android.camera.ChatwalaActivity;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/19/2014
 * Time: 4:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class CwNotificationManager {
    private static final int NEW_MESSAGE_ID = 1;

    private ChatwalaApplication app;
    private NotificationManager nm;

    private CwNotificationManager() {}

    private static class Singleton {
        public static final CwNotificationManager instance = new CwNotificationManager();
    }

    public static CwNotificationManager attachToApp(ChatwalaApplication app) {
        me().app = app;
        me().nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        return Singleton.instance;
    }

    private static CwNotificationManager me() {
        return Singleton.instance;
    }

    private ChatwalaApplication getApp() {
        return app;
    }

    private NotificationManager getNotificationManager() {
        return nm;
    }

    public static void makeNewMessagesNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(me().getApp())
                .setSmallIcon(R.drawable.appicon)
                .setContentTitle(me().getApp().getString(R.string.notification_new_message_title))
                .setContentText(me().getApp().getString(R.string.notification_new_message_content))
                .setAutoCancel(true);

        Intent intent = new Intent(me().getApp(), ChatwalaActivity.class);
        intent.putExtra(ChatwalaActivity.OPEN_DRAWER_EXTRA, true);
        PendingIntent pi = PendingIntent.getActivity(me().getApp(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pi);

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(me().getApp().getString(R.string.notification_new_message_content));
        builder.setStyle(bigText);

        builder.setVibrate(new long[] { 0, 250, 100, 250 });
        builder.setSound(Uri.parse("android.resource://" + me().getApp().getPackageName() + "/raw/chatwala"), AudioManager.STREAM_NOTIFICATION);

        if(!AppPrefs.getKillswitch().isActive()) {
            me().getNotificationManager().notify(NEW_MESSAGE_ID, builder.build());
        }
    }

    public static void removeNewMessagesNotification() {
        me().getNotificationManager().cancel(NEW_MESSAGE_ID);
    }
}
