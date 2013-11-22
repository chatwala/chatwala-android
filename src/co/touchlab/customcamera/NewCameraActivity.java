package co.touchlab.customcamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import co.touchlab.customcamera.util.CameraUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 11/18/13
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewCameraActivity extends Activity
{
    CroppingLayout cameraPreviewContainer, videoViewContainer;
    CameraPreviewView cameraPreviewView;

    private Button mainActionButton;
    private AtomicBoolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.crop_test);

        isRecording = new AtomicBoolean(false);

        cameraPreviewContainer = (CroppingLayout)findViewById(R.id.surface_view_container);
        videoViewContainer = (CroppingLayout)findViewById(R.id.video_view_container);
        mainActionButton = (Button) findViewById(R.id.camera_button);
        mainActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                triggerButtonAction();
            }
        });
    }

    private void triggerButtonAction()
    {
        if (isRecording.compareAndSet(false, true))
        {
            cameraPreviewView.startRecording();
        }
        else
        {
            isRecording.set(false);
            cameraPreviewView.stopRecording();
        }
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        createSurface();
    }



    @Override
    protected void onPause()
    {
        super.onDestroy();
        tearDownSurface();
    }


    private void createSurface()
    {
        cameraPreviewView = new CameraPreviewView(NewCameraActivity.this);
        cameraPreviewContainer.addView(cameraPreviewView);
    }


    private void tearDownSurface()
    {
        cameraPreviewContainer.removeAllViews();
        cameraPreviewView.releaseResources();
        cameraPreviewView = null;
    }
}
