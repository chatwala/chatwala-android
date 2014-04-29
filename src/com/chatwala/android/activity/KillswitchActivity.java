package com.chatwala.android.activity;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.R;
import com.chatwala.android.util.KillswitchInfo;
import com.squareup.picasso.Picasso;

/**
 * Created by matthewdavis on 1/9/14.
 */
public class KillswitchActivity extends BaseChatWalaActivity {
    private KillswitchInfo killswitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_killswitch);

        killswitch = AppPrefs.getInstance(this).getKillswitch();
        if(killswitch == null || !killswitch.isActive()) {
            NewCameraActivity.startMe(this);
        }
        else {
            TextView killswitchText = (TextView) findViewById(R.id.killswitch_text);
            killswitchText.setText(killswitch.getCopy());
            //killswitchText.setTypeface(((ChatwalaApplication) getApplication()).fontMd);

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int imageSize = Math.round(size.x * .66f);

            Picasso.with(this)
                    .load(R.drawable.killswitch)
                    .resize(imageSize, imageSize)
                    .noFade()
                    .into((ImageView) findViewById(R.id.killswitch_icon));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(killswitch == null || !killswitch.isActive()) {
            NewCameraActivity.startMe(this);
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
