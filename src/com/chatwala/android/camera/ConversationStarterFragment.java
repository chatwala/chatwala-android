package com.chatwala.android.camera;

import android.os.Environment;
import android.widget.Toast;
import com.chatwala.android.util.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Eliezer on 4/23/2014.
 */
public class ConversationStarterFragment extends ChatwalaFragment {
    private CWCamera camera;
    private ChatwalaRecordingTexture recordingSurface;

    @Override
    public void onSurfaceCreated(ChatwalaRecordingTexture recordingSurface) {
        Logger.i("Surface is ready for ConversationStarterFragment");
        this.recordingSurface = recordingSurface;
        addViewToTop(recordingSurface, false);
    }

    @Override
    public void onCameraReady(CWCamera camera) {
        Logger.i("Camera is ready for ConversationStarterFragment");
        this.camera = camera;
    }

    @Override
    public void onActionButtonClicked() {
        if(!camera.startRecording()) {
            Toast.makeText(getActivity(), "Couldn't start recording", Toast.LENGTH_LONG).show();
            return;
        }
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(!camera.stopRecording()) {
                    //Toast.makeText(getActivity(), "Couldn't stop recording", Toast.LENGTH_LONG).show();
                }
                else {
                    try {
                        org.apache.commons.io.IOUtils.copy(new FileInputStream(camera.getRecordingFile()), new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/test.mp4"));
                    }
                    catch(Exception e) {

                        Logger.e("Couldn't copy video", e);
                    }
                }
            }
        }, 10650);
    }

    @Override
    protected void onTopFragmentClicked() {
        if(camera != null && !camera.hasError()) {
            if(camera.toggleCamera(recordingSurface.getWidth(), recordingSurface.getHeight())) {
                camera.attachToPreview(recordingSurface.getSurfaceTexture());
            }
        }
    }

    @Override
    protected void onBottomFragmentClicked() {

    }
}
