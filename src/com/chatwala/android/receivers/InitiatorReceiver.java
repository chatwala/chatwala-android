package com.chatwala.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.chatwala.android.activity.NewCameraActivity;

/**
 * Created by Eliezer on 3/4/14.
 */
public class InitiatorReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent initiatorIntent = new Intent(context, NewCameraActivity.class);
        initiatorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        initiatorIntent.putExtra(NewCameraActivity.INITIATOR_EXTRA, intent.getData().getLastPathSegment());
        context.startActivity(initiatorIntent);
    }
}
