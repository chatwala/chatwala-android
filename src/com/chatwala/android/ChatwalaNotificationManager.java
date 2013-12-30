package com.chatwala.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/30/13
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaNotificationManager
{
    private static final int newMessagesNotificationId = 0;

    public static void makeNewMessagesNotification(Context context)
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle("Chatwala")
                        .setContentText("You have been sent a message!")
                        .setAutoCancel(true);
        Intent resultIntent = new Intent(context, NewCameraActivity.class);
        resultIntent.putExtra(NewCameraActivity.OPEN_DRAWER, true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        //stackBuilder.addParentStack();
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(newMessagesNotificationId, mBuilder.build());
    }

    public static void removeNewMessagesNotification(Context context)
    {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(newMessagesNotificationId);
    }

    public static void makeErrorInitialSendNotification(Context context, File pendingMessageFile)
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle("Chatwala Error")
                        .setContentText("There was a problem sending your message. Tap here to retry.")
                        .setAutoCancel(true);
        Intent resultIntent = new Intent(context, NewCameraActivity.class);
        resultIntent.putExtra(NewCameraActivity.PENDING_SEND_URL, pendingMessageFile.getAbsolutePath());

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        //stackBuilder.addParentStack();
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, mBuilder.build());
    }
}
