package co.touchlab.customcamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import co.touchlab.customcamera.util.CameraUtils;
import co.touchlab.customcamera.util.ShareUtils;
import co.touchlab.customcamera.util.ZipUtil;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
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
    private AtomicBoolean messageLoadComplete = new AtomicBoolean(false);
    private AtomicBoolean previewReady = new AtomicBoolean(false);
    private ChatMessage chatMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.crop_test);

        isRecording = new AtomicBoolean(false);

        cameraPreviewContainer = (CroppingLayout) findViewById(R.id.surface_view_container);
        videoViewContainer = (CroppingLayout) findViewById(R.id.video_view_container);
        mainActionButton = (Button) findViewById(R.id.camera_button);
        mainActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                triggerButtonAction();
            }
        });

        //Kick off attachment load
        new MessageLoaderTask().execute();
    }

    private void triggerButtonAction()
    {
        if (isRecording.compareAndSet(false, true))
        {
            cameraPreviewView.startRecording();
            mainActionButton.setText("Stop");
        }
        else
        {
            isRecording.set(false);
            cameraPreviewView.stopRecording();
            mainActionButton.setText("Start");
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        disableInterface();
        createSurface();
    }

    private void disableInterface()
    {
        mainActionButton.setActivated(false);
    }

    private void enableInterface()
    {
        mainActionButton.setActivated(true);
        mainActionButton.setText("Start");
    }

    private void messageLoaded(ChatMessage message)
    {
        this.chatMessage = message;
        messageLoadComplete.set(true);
        liveForRecording();
    }

    private void previewSurfaceReady()
    {
        previewReady.set(true);
        liveForRecording();
    }

    private void liveForRecording()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(previewReady.get() && messageLoadComplete.get())
                {
                    enableInterface();
                }
            }
        });
    }

    @Override
    protected void onPause()
    {
        super.onDestroy();
        tearDownSurface();
    }

    private void createSurface()
    {
        cameraPreviewView = new CameraPreviewView(NewCameraActivity.this, new CameraPreviewView.CameraPreviewCallback()
        {
            @Override
            public void surfaceReady()
            {
                previewSurfaceReady();
            }
        });
        cameraPreviewContainer.addView(cameraPreviewView);
    }

    private void tearDownSurface()
    {
        cameraPreviewContainer.removeAllViews();
        cameraPreviewView.releaseResources();
        cameraPreviewView = null;
    }

    class MessageLoaderTask extends AsyncTask<Void, Void, ChatMessage>
    {
        @Override
        protected ChatMessage doInBackground(Void... params)
        {
            return ShareUtils.extractFileAttachment(NewCameraActivity.this);
        }

        @Override
        protected void onPostExecute(ChatMessage chatMessage)
        {
            messageLoaded(chatMessage);
        }
    }
}
