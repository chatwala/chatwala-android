package com.chatwala.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.R;
import com.chatwala.android.loaders.BroadcastSender;

/**
 * Created by matthewdavis on 1/9/14.
 */
public class KillswitchActivity extends BaseChatWalaActivity
{
    private static final String KILLSWITCH_TEXT = "KILLSWITCH_TEXT";

    private BroadcastReceiver killswitchReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_killswitch);

        ((TextView)findViewById(R.id.killswitch_text)).setText(getIntent().getStringExtra(KILLSWITCH_TEXT));

        killswitchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NewCameraActivity.startMe(KillswitchActivity.this);
                finish();
            }
        };

        LocalBroadcastManager.getInstance(KillswitchActivity.this).registerReceiver(killswitchReceiver, new IntentFilter(BroadcastSender.KILLSWITCH_OFF_BROADCAST));
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        LocalBroadcastManager.getInstance(KillswitchActivity.this).unregisterReceiver(killswitchReceiver);
        killswitchReceiver = null;
    }

    public static void startMe(Context context, String displayText)
    {
        if(ChatwalaApplication.isKillswitchShowing.compareAndSet(false, true))
        {
            Intent intent = new Intent(context, KillswitchActivity.class);
            intent.putExtra(KILLSWITCH_TEXT, displayText);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    @Override
    public void onBackPressed()
    {
        //Do nothing, no getting out of the killswitch.
    }
}
