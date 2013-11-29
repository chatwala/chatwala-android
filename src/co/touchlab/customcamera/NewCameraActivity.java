package co.touchlab.customcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import co.touchlab.customcamera.util.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

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
    public static final int RECORDING_TIME = 10000;

    public enum AppState
    {
        Off, Transition, LoadingFileCamera, ReadyStopped, PlaybackOnly, /*PlaybackRecording,*/ RecordingLimbo, Recording, PreviewLoading, PreviewReady
    }

    // ************* onCreate only *************
    private CroppingLayout cameraPreviewContainer, videoViewContainer;
    private DynamicVideoView messageVideoView;
    private DynamicVideoView recordPreviewVideoView;
    private View closeRecordPreviewView;
    private TextView timerText;
    // ************* onCreate only *************


    // ************* SEMI DANGEROUS STATE *************
    private AtomicBoolean messageLoadComplete = new AtomicBoolean(false);
    private AtomicBoolean cameraPreviewReady = new AtomicBoolean(false);
    // ************* SEMI DANGEROUS STATE *************


    // ************* DANGEROUS STATE *************
    private CameraPreviewView cameraPreviewView;
    private ChatMessage chatMessage;
    private long videoPlaybackDuration = 0;
    private File recordPreviewFile;
    private AppState appState = AppState.Off;
    // ************* DANGEROUS STATE *************



    public synchronized AppState getAppState()
    {
        return appState;
    }

    public synchronized void setAppState(AppState appState)
    {
        CWLog.i(NewCameraActivity.class, "setAppState: " + appState);
        this.appState = appState;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.crop_test);

        cameraPreviewContainer = (CroppingLayout) findViewById(R.id.surface_view_container);
        videoViewContainer = (CroppingLayout) findViewById(R.id.video_view_container);
        View timerButtonContainer = findViewById(R.id.timerContainer);
        timerText = (TextView) findViewById(R.id.timerText);
        timerButtonContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                triggerButtonAction();
            }
        });

        closeRecordPreviewView = findViewById(R.id.closeVideoPreview);
        closeRecordPreviewView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                closeResultPreview();
            }
        });


    }

    @Override
    protected void onResume()
    {
        super.onResume();

        setAppState(AppState.Transition);

        //Kick off attachment load
        initStartState();
        createSurface();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        setAppState(AppState.Off);
        tearDownSurface();
    }

    private void initStartState()
    {
        setAppState(AppState.LoadingFileCamera);
        new MessageLoaderTask().execute();
    }

    private void triggerButtonAction()
    {
        //Don't do anything.  These should be very short states.
        AppState state = getAppState();
        if (state == AppState.Off || state == AppState.Transition || state == AppState.LoadingFileCamera || state == AppState.RecordingLimbo || state == AppState.PreviewLoading)
            return;

        if (state == AppState.ReadyStopped)
        {
            startPlaybackRecording();
            return;
        }

        if (state == AppState.PlaybackOnly || /*state == AppState.PlaybackRecording ||*/ state == AppState.Recording)
        {
            throw new RuntimeException("do this");
        }

        if (state == AppState.PreviewReady)
        {
            sendEmail(recordPreviewFile);
            closeResultPreview();
        }
    }

    class HeartbeatTimer extends Thread
    {
        private long startRecordingTime;
        private boolean recordingStarted;
        private long endRecordingTime;
        private long startTime = System.currentTimeMillis();

        HeartbeatTimer(long startRecordingTime, boolean recordingStarted)
        {
            this.startRecordingTime = startRecordingTime;
            this.recordingStarted = recordingStarted;
            endRecordingTime = startRecordingTime + RECORDING_TIME;
        }

        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    Thread.sleep(20);
                }
                catch (InterruptedException e)
                {
                }

                long now = System.currentTimeMillis() - startTime;
                if (!recordingStarted && now >= startRecordingTime)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            startRecording();
                        }
                    });
                }

                if (now >= endRecordingTime)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            stopRecording();
                        }
                    });
                    break;
                }
            }
        }
    }

    private void startPlaybackRecording()
    {
        AndroidUtils.isMainThread();
        timerText.setText("Stop");
        int recordingStartMillis = chatMessage == null ? 0 : (int) Math.round(chatMessage.metadata.startRecording * 1000);
        if (messageVideoView != null)
        {
            setAppState(AppState.PlaybackOnly);
            messageVideoView.seekTo(0);
            messageVideoView.start();
        }

        if (recordingStartMillis == 0)
        {
            startRecording();
        }

        new HeartbeatTimer(recordingStartMillis, recordingStartMillis == 0).start();
    }

    private void startRecording()
    {
        setAppState(AppState.RecordingLimbo);
        cameraPreviewView.startRecording();
    }

    private void stopRecording()
    {
        AndroidUtils.isMainThread();
        setAppState(AppState.PreviewLoading);
        cameraPreviewView.stopRecording();
        timerText.setText("Start");
    }

    private void messageLoaded(ChatMessage message)
    {
        this.chatMessage = message;

        if (chatMessage == null || chatMessage.messageVideo == null)
            messageVideoLoaded();
        else
        {
            new LoadAndShowVideoMessageTask(false).execute(chatMessage.messageVideo);
        }
    }

    private void messageVideoLoaded()
    {
        messageLoadComplete.set(true);
        liveForRecording();
    }

    private void previewSurfaceReady()
    {
        cameraPreviewReady.set(true);
        liveForRecording();
    }

    private void liveForRecording()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (cameraPreviewReady.get() && messageLoadComplete.get())
                {
                    setAppState(AppState.ReadyStopped);
                }
            }
        });
    }

    class VideoInfo
    {
        public File videoFile;
        public int width;
        public int height;
        public int rotation;
        public Bitmap bitmap;
    }

    class LoadAndShowVideoMessageTask extends AsyncTask<File, Void, VideoInfo>
    {
        private boolean previewVideo;

        LoadAndShowVideoMessageTask(boolean previewVideo)
        {
            this.previewVideo = previewVideo;
        }

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
                if (!previewVideo)
                    videoPlaybackDuration = Long.parseLong(duration);
                String rotation = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

                inp.close();

                VideoInfo videoInfo = new VideoInfo();
                videoInfo.videoFile = videoFile;
                videoInfo.bitmap = VideoUtils.createVideoFrame(videoFile.getPath(), 0);
                videoInfo.rotation = rotation == null ? 0 : Integer.parseInt(rotation);
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

            if (previewVideo)
            {
                recordPreviewVideoView = new DynamicVideoView(NewCameraActivity.this, recordPreviewFile, videoInfo.width, videoInfo.height, videoInfo.rotation);

                cameraPreviewContainer.addView(recordPreviewVideoView);
                closeRecordPreviewView.setVisibility(View.VISIBLE);
                recordPreviewVideoView.start();
                recordPreviewVideoView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (recordPreviewVideoView.isPlaying())
                            recordPreviewVideoView.pause();
                        else
                            recordPreviewVideoView.start();
                    }
                });
                timerText.setText("Share");
                setAppState(AppState.PreviewReady);
            }
            else
            {
                messageVideoView = new DynamicVideoView(NewCameraActivity.this, videoInfo.videoFile, videoInfo.width, videoInfo.height, videoInfo.rotation);
                videoViewContainer.addView(messageVideoView);

                messageVideoLoaded();
            }
        }
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
            public void recordingStarted()
            {
                setAppState(AppState.Recording);
            }

            @Override
            public void recordingDone(File videoFile)
            {
                showResultPreview(videoFile);
            }
        });
        cameraPreviewContainer.addView(cameraPreviewView);
    }

    private void showResultPreview(File videoFile)
    {
        this.recordPreviewFile = videoFile;
        tearDownSurface();
        new LoadAndShowVideoMessageTask(true).execute(recordPreviewFile);
    }

    private void closeResultPreview()
    {
        recordPreviewFile = null;
        cameraPreviewContainer.removeView(recordPreviewVideoView);
        closeRecordPreviewView.setVisibility(View.GONE);
        initStartState();
        createSurface();
    }

    private void tearDownSurface()
    {
        cameraPreviewContainer.removeAllViews();
        if (cameraPreviewView != null)
        {
            cameraPreviewView.releaseResources();
            cameraPreviewView = null;
        }
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
                File buildDir = new File(CameraUtils.getRootDataFolder(NewCameraActivity.this), "chat_" + System.currentTimeMillis());
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

                    String myEmail = AppPrefs.getInstance(NewCameraActivity.this).getPrefSelectedEmail();

                    if (myEmail == null && chatMessage != null)
                        myEmail = chatMessage.probableEmailSource;

                    sendMessageMetadata.senderId = myEmail;

                    FileWriter fileWriter = new FileWriter(metadataFile);

                    fileWriter.append(sendMessageMetadata.toJsonString());

                    fileWriter.close();

                    File shareDir = new File(NewCameraActivity.this.getExternalFilesDir(null), "sharefile_" + System.currentTimeMillis());
                    shareDir.mkdirs();
                    outZip = new File(shareDir, "chat.wala");

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
                String toEmail = null;

                if (chatMessage != null)
                {
                    toEmail = chatMessage.metadata.senderId;
                }

                if (toEmail != null)
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{toEmail});

                intent.putExtra(Intent.EXTRA_SUBJECT, "test reply");
                intent.putExtra(Intent.EXTRA_TEXT, "the video");

                Uri uri = Uri.fromFile(outZip);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(intent, "Send email..."));
            }
        }.execute();
    }
}
