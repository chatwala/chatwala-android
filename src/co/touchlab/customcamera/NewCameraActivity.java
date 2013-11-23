package co.touchlab.customcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import co.touchlab.customcamera.ui.TimerDial;
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

    private AtomicBoolean isRecording;
    private AtomicBoolean messageLoadComplete = new AtomicBoolean(false);
    private AtomicBoolean previewReady = new AtomicBoolean(false);
    private boolean initialMessageDone;
    private ChatMessage chatMessage;

    private long videoPlaybackDuration = 0;
//    private long myMessageStartTime;
//    private long myMessageEndTime;
//    private long replyMessageEndTime;

    private View mainActionButton;
    private TimerDial timerDial;
    private DynamicVideoView dynamicVideoView;
    private TextView timerText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.crop_test);

        isRecording = new AtomicBoolean(false);

        cameraPreviewContainer = (CroppingLayout) findViewById(R.id.surface_view_container);
        videoViewContainer = (CroppingLayout) findViewById(R.id.video_view_container);
        mainActionButton = findViewById(R.id.timerContainer);
        timerDial = (TimerDial) findViewById(R.id.timerDial);
        timerText = (TextView) findViewById(R.id.timerText);
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
        if (!isRecording.get())
        {
            startTimer();
        }
        else
        {
            timerDial.stopAnimation();
        }
    }

    private void startTimer()
    {
        isRecording.set(true);
        timerText.setText("Stop");
        timerDial.startAnimation(new TimerDial.TimerCallback()
        {
            @Override
            public void countdownComplete()
            {
                if (dynamicVideoView != null)
                {
                    dynamicVideoView.start();
                }
            }

            @Override
            public void playbackComplete()
            {

            }

            @Override
            public void recordStart()
            {
                cameraPreviewView.startRecording();
            }

            @Override
            public void recordComplete()
            {
                stopRecording();
            }
        }, 3000, (int) videoPlaybackDuration, chatMessage == null ? 0 : (int)Math.round(chatMessage.metadata.startRecording * 1000), 10000);
    }

    private void stopRecording()
    {
        isRecording.set(false);
        cameraPreviewView.stopRecording();
        timerText.setText("Start");
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
        timerText.setText("Start");
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
                    showMessageVideo();
                }
            }
        });
    }

    private void showMessageVideo()
    {
        if (!initialMessageDone && chatMessage != null && chatMessage.messageVideo != null)
        {
            initialMessageDone = true;
            new LoadAndShowVideoMessageTask().execute(chatMessage.messageVideo);
        }
    }

    class VideoInfo
    {
        public File videoFile;
        public int width;
        public int height;
    }

    class LoadAndShowVideoMessageTask extends AsyncTask<File, Void, VideoInfo>
    {
        @Override
        protected VideoInfo doInBackground(File... params)
        {
            try
            {
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                File videoFile = params[0];
                FileInputStream inp = new FileInputStream(videoFile);

                metaRetriever.setDataSource(inp.getFD());
                String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                videoPlaybackDuration = Long.parseLong(duration);
                String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

                inp.close();

                VideoInfo videoInfo = new VideoInfo();
                videoInfo.videoFile = videoFile;
                videoInfo.width = Integer.parseInt(width);
                videoInfo.height = Integer.parseInt(height);

                return videoInfo;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

        }

        @Override
        protected void onPostExecute(VideoInfo videoInfo)
        {
            dynamicVideoView = new DynamicVideoView(NewCameraActivity.this, videoInfo.videoFile, videoInfo.width, videoInfo.height);
            videoViewContainer.addView(dynamicVideoView);
            dynamicVideoView.setVideoPath(videoInfo.videoFile.getPath());
//            dynamicVideoView.start();
            startTimer();
        }
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
        videoViewContainer.removeAllViews();
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

                    long startRecordingMillis = openedMessageMetadata == null ? 0 : Math.round(openedMessageMetadata.startRecording * 1000d);
                    sendMessageMetadata.startRecording = ((double) Math.max(videoPlaybackDuration - startRecordingMillis, 0)) / 1000d;

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
