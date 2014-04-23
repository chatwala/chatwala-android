package com.chatwala.android.camera;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.R;
import com.chatwala.android.activity.DrawerListActivity;

/**
 * Created by Eliezer on 4/18/2014.
 */
public class ChatwalaActivity extends DrawerListActivity {
    private ChatwalaFragment currentFragment;
    private CWCamera camera;
    private AcquireCameraAsyncTask acquireCameraTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main_activity);

        loadCamera();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(camera == null) {
            loadCamera();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(acquireCameraTask != null) {
            acquireCameraTask.cancel(true);
        }
    }

    public ChatwalaApplication getApp() {
        return ((ChatwalaApplication) getApplication());
    }

    private void loadCamera() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        acquireCameraTask = new AcquireCameraAsyncTask(this, new AcquireCameraAsyncTask.OnCameraReadyListener() {
            @Override
            public void onCameraReady(CWCamera camera) {
                acquireCameraTask = null;
                ChatwalaActivity.this.camera = camera;
            }
        }, dm.widthPixels, dm.heightPixels);
        acquireCameraTask.execute();
    }

    private void swapFragment(Fragment newFragment, String tag, boolean addToBackStack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft = ft.replace(R.id.main_fragment_container, newFragment, tag);
        if(addToBackStack) {
            ft.addToBackStack(null);
        }
        ft.commit();
    }

    @Override
    protected void performAddButtonAction() {

    }
}
