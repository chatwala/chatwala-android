package com.chatwala.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.R;
import com.chatwala.android.util.KillswitchInfo;

/**
 * Created by matthewdavis on 1/9/14.
 */
public class KillswitchActivity extends BaseChatWalaActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_killswitch);

        KillswitchInfo killswitch = AppPrefs.getInstance(this).getKillswitch();
        if(!killswitch.isActive()) {
            NewCameraActivity.startMe(this);
        }
        else {
            ((TextView) findViewById(R.id.killswitch_text)).setText(killswitch.getCopy());
        }
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
}
