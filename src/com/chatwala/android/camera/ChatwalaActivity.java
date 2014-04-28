package com.chatwala.android.camera;

import android.graphics.SurfaceTexture;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.R;
import com.chatwala.android.activity.DrawerListActivity;
import com.chatwala.android.ui.CWButton;
import com.chatwala.android.ui.PacmanView;
import com.chatwala.android.util.Logger;

import java.io.File;

/**
 * Created by Eliezer on 4/18/2014.
 */
public class ChatwalaActivity extends DrawerListActivity implements TextureView.SurfaceTextureListener {
    private ChatwalaFragment currentFragment;
    private ChatwalaFragment conversationStarterFragment;
    private CWCamera camera;
    private ChatwalaRecordingTexture recordingSurface;
    private AcquireCameraAsyncTask acquireCameraTask;
    private CWButton actionButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatwala_activity);

        loadCamera();

        conversationStarterFragment = new ConversationStarterFragment();
        currentFragment = conversationStarterFragment;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft = ft.add(R.id.chatwala_fragment_container, currentFragment, "conversation_starter");
        ft.commit();

        actionButton = (CWButton) findViewById(R.id.chatwala_button);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentFragment.onActionButtonClicked();
            }
        });
        actionButton.bringToFront();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(acquireCameraTask == null && camera == null) {
            loadCamera();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Logger.i("Pausing...");

        if(acquireCameraTask != null) {
            acquireCameraTask.cancel(true);
        }
        if(camera != null) {
            camera.release();
            camera = null;
        }
        finish();
    }

    public ChatwalaApplication getApp() {
        return ((ChatwalaApplication) getApplication());
    }

    private void loadCamera() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        recordingSurface = new ChatwalaRecordingTexture(ChatwalaActivity.this);
        acquireCameraTask = new AcquireCameraAsyncTask(new AcquireCameraAsyncTask.OnCameraReadyListener() {
            @Override
            public void onCameraReady(CWCamera camera) {
                acquireCameraTask = null;

                if(camera.hasError()) {
                    //do something
                    Logger.e("Camera loaded with error");
                    return;
                }
                ChatwalaActivity.this.camera = camera;
                recordingSurface.setSurfaceTextureListener(ChatwalaActivity.this);
                currentFragment.onSurfaceCreated(recordingSurface);
                if (recordingSurface.isAvailable()) {
                    onSurfaceTextureAvailable(recordingSurface.getSurfaceTexture(), recordingSurface.getWidth(), recordingSurface.getHeight());
                }
                recordingSurface.forceOnMeasure();
                Logger.i("Camera loaded");
            }
        }, dm.widthPixels, dm.heightPixels / 2);
        acquireCameraTask.execute();
        Logger.i("Loading camera");
    }

    private void swapFragment(Fragment newFragment, String tag, boolean addToBackStack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft = ft.replace(R.id.chatwala_fragment_container, newFragment, tag);
        if(addToBackStack) {
            ft.addToBackStack(null);
        }
        ft.commit();
    }

    @Override
    protected void performAddButtonAction() {

    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
        Logger.i("A surface is available");
        if(camera != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Logger.i("Surface ready and camera is good to go");
                    camera.attachToPreview(surface);
                    currentFragment.onCameraReady(camera);
                }
            }, 100);
        }
        else {
            Logger.w("Surface ready and camera is not good to go...attempting to reload camera");
            loadCamera();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //do nothing...camera handles it
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Logger.i("Surface is getting destroyed");
        if(camera != null) {
            if (camera.isRecording()) {
                camera.stopRecording(true);
            }
            camera.stopPreview();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //do nothing...camera handles it
    }

    public boolean startRecording(int maxRecordingMillis) {
        MediaRecorder.OnInfoListener listener = new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    if(camera != null) {
                        Logger.d("Reached max recording duration and we still have the camera");
                        stopRecording(false);
                    }
                    else {
                        currentFragment.onRecordingFinished(null);
                        Logger.d("Reached max recording duration and we don't have the camera");
                    }
                }
            }
        };

        if(!camera.canRecord() || !camera.startRecording(maxRecordingMillis, listener)) {
            Logger.e("Couldn't start recording");
            return false;
        }
        else {
            PacmanView pacman = new PacmanView(this);
            addActionView(pacman, 0);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            pacman.setLayoutParams(lp);
            pacman.startAnimation(maxRecordingMillis);
            return true;
        }
    }

    public boolean stopRecording() {
        return stopRecording(true);
    }

    private boolean stopRecording(boolean actuallyStop) {
        if(camera != null) {
            ((PacmanView) getActionViewAt(0)).stopAndRemove();
            File recordedFile = camera.stopRecording(actuallyStop);
            currentFragment.onRecordingFinished(recordedFile);
            return true;
        }
        else {
            return false;
        }
    }

    public void setActionView(View v) {
        actionButton.setActionView(v);
    }

    public void addActionView(View v) {
        actionButton.addActionView(v);
    }

    public void addActionView(View v, int position) {
        actionButton.addActionView(v, position);
    }

    public View getActionViewAt(int position) {
        return actionButton.getActionViewAt(position);
    }

    public void removeActionViewAt(int position) {
        actionButton.removeActionViewAt(position);
    }

    public void clearActionView() {
        actionButton.clearActionView();
    }
}
