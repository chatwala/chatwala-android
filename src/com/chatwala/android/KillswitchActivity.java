package com.chatwala.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.superbus.CheckKillswitchCommand;

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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                BroadcastSender.makeKillswitchOffBroadcast(KillswitchActivity.this);
            }
        }, 10000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DataProcessor.runProcess(new Runnable() {
            @Override
            public void run() {
                BusHelper.submitCommandSync(KillswitchActivity.this, new CheckKillswitchCommand());
            }
        });
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
        Intent intent = new Intent(context, KillswitchActivity.class);
        intent.putExtra(KILLSWITCH_TEXT, displayText);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
