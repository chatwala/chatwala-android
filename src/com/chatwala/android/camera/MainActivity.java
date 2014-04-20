package com.chatwala.android.camera;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import com.chatwala.android.R;

/**
 * Created by Eliezer on 4/18/2014.
 */
public class MainActivity extends FragmentActivity {
    private CWCamera camera;
    private AcquireCameraAsyncTask acquireCameraTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        acquireCameraTask = new AcquireCameraAsyncTask(new AcquireCameraAsyncTask.OnCameraReadyListener() {
            @Override
            public void onCameraReady(CWCamera camera) {
                MainActivity.this.camera = camera;
            }
        });
        acquireCameraTask.execute();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(camera == null) {
            acquireCameraTask = new AcquireCameraAsyncTask(new AcquireCameraAsyncTask.OnCameraReadyListener() {
                @Override
                public void onCameraReady(CWCamera camera) {
                    MainActivity.this.camera = camera;
                }
            });
            acquireCameraTask.execute();
        }
    }

    private void swapFragment(Fragment newFragment, String tag, boolean addToBackStack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft = ft.replace(R.id.main_top_fragment_container, newFragment, tag);
        if(addToBackStack) {
            ft.addToBackStack(null);
        }
        ft.commit();
    }
}
