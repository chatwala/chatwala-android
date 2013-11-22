package co.touchlab.customcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import java.util.Arrays;
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

    private long myMessageStartTime;
    private long myMessageEndTime;
    private long replyMessageEndTime;

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
                if (previewReady.get() && messageLoadComplete.get())
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

            @Override
            public void recordingDone(File videoFile)
            {
                sendEmail(videoFile);
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

    @SuppressWarnings("unchecked")
    private void sendEmail(final File videoFile)
    {
        new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object... params)
            {
                File buildDir = new File(Environment.getExternalStorageDirectory(), "chat_" + System.currentTimeMillis());
                buildDir.mkdirs();

                File walaFile = new File(buildDir, "video.mp4");

                File outZip;
                try
                {

                    FileOutputStream output = new FileOutputStream(walaFile);
                    FileInputStream input = new FileInputStream(videoFile);
                    IOUtils.copy(input, output);

                    input.close();
                    output.close();

                    File metadataFile = new File(buildDir, "metadata.json");

                    MessageMetadata openedMessageMetadata = chatMessage == null ? null : chatMessage.metadata;

                    MessageMetadata sendMessageMetadata = openedMessageMetadata == null ? new MessageMetadata() : openedMessageMetadata.copy();

                    sendMessageMetadata.incrementForNewMessage();

                    long startRecordingMillis = replyMessageEndTime == 0 ? 0 : replyMessageEndTime - myMessageStartTime;
                    sendMessageMetadata.startRecording = ((double) startRecordingMillis) / 1000d;

                    sendMessageMetadata.senderId = "kevin@touchlab.co";

                    FileWriter fileWriter = new FileWriter(metadataFile);

                    fileWriter.append(sendMessageMetadata.toJsonString());

                    fileWriter.close();

                    outZip = new File(Environment.getExternalStorageDirectory(), "chat.wala");

                    ZipUtil.zipFiles(outZip, Arrays.asList(buildDir.listFiles()));
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                catch (JSONException e)
                {
                    throw new RuntimeException(e);
                }

                return outZip;
            }

            @Override
            protected void onPostExecute(Object o)
            {
                File outZip = (File) o;
                Intent intent = new Intent(Intent.ACTION_SEND);

                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"kevin@touchlab.co"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "test reply");
                intent.putExtra(Intent.EXTRA_TEXT, "the video");

                Uri uri = Uri.fromFile(outZip);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(intent, "Send email..."));
            }
        }.execute();
    }
}
