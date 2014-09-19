package com.chatwala.android.app;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.chatwala.android.R;
import com.chatwala.android.camera.ChatwalaActivity;
import com.chatwala.android.util.KillswitchInfo;
import com.koushikdutta.ion.Ion;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class KillswitchActivity extends BaseChatwalaActivity {
    private KillswitchInfo killswitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.killswitch_activity);

        killswitch = AppPrefs.getKillswitch();
        if(killswitch == null || !killswitch.isActive()) {
            startChatwalaActivity();
        }
        else {
            TextView killswitchText = (TextView) findViewById(R.id.killswitch_text);
            killswitchText.setText(killswitch.getCopy());
            //killswitchText.setTypeface(((ChatwalaApplication) getApplication()).fontMd);

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int imageSize = Math.round(size.x * .66f);

            Ion.with((ImageView) findViewById(R.id.killswitch_icon))
                    .error(R.drawable.killswitch)
                    .resize(imageSize, imageSize)
                    .load(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        killswitch = AppPrefs.getKillswitch();
        if(killswitch == null || !killswitch.isActive()) {
            //startChatwalaActivity();
        }
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    private void startChatwalaActivity() {
        Intent chatwalaIntent = new Intent(this, ChatwalaActivity.class);
        chatwalaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(chatwalaIntent);
        finish();
    }
}
