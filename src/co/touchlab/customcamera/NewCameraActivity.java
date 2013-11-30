package co.touchlab.customcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import co.touchlab.customcamera.ui.TimerDial;
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
    private View coverAnimation;


    public enum AppState
    {
        Off, Transition, LoadingFileCamera, ReadyStopped, PlaybackOnly, /*PlaybackRecording,*/ RecordingLimbo, Recording, PreviewLoading, PreviewReady
    }

    // ************* onCreate only *************
    private CroppingLayout cameraPreviewContainer, videoViewContainer;
    private View cameraPreviewFriendReplyText;
    private View messageVideoFriendReplyText;
    private DynamicVideoView messageVideoView;
    private DynamicVideoThumbImageView messageVideoThumbnailView;
    private DynamicVideoView recordPreviewVideoView;
    private View closeRecordPreviewView;
    private ImageView timerKnob;
    private TimerDial timerDial;
    // ************* onCreate only *************


    // ************* SEMI DANGEROUS STATE *************
//    private AtomicBoolean messageLoadComplete = new AtomicBoolean(false);
//    private AtomicBoolean cameraPreviewReady = new AtomicBoolean(false);
    // ************* SEMI DANGEROUS STATE *************


    // ************* DANGEROUS STATE *************
    private CameraPreviewView cameraPreviewView;
    private ChatMessage chatMessage;
    private VideoUtils.VideoMetadata chatMessageVideoMetadata;
    private Bitmap chatMessageVideoBitmap;
    private File recordPreviewFile;
    private AppState appState = AppState.Off;
    private HeartbeatTimer heartbeatTimer;
    // ************* DANGEROUS STATE *************


    public synchronized AppState getAppState()
    {
        return appState;
    }

    public synchronized void setAppState(AppState appState)
    {
        CWLog.i(NewCameraActivity.class, "setAppState: " + appState + " (" + System.currentTimeMillis() + ")");
        AndroidUtils.isMainThread();
        this.appState = appState;

        switch (this.appState)
        {
            case Transition:
            case LoadingFileCamera:
//            case RecordingLimbo:
//            case PreviewLoading:
                coverAnimation.setVisibility(View.VISIBLE);
                break;
            default:
                coverAnimation.setVisibility(View.GONE);
        }

        switch (this.appState)
        {
            case ReadyStopped:
                timerKnob.setVisibility(View.VISIBLE);
//                timerKnob.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out_and_in));
                timerKnob.setImageResource(R.drawable.ic_action_video);
                break;
            case PlaybackOnly:
            case Recording:
                timerKnob.setVisibility(View.VISIBLE);
//                timerKnob.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out_and_in));
                timerKnob.setImageResource(R.drawable.ic_action_playback_stop);
                break;
            case PreviewReady:
                timerKnob.setVisibility(View.VISIBLE);
//                timerKnob.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out_and_in));
                timerKnob.setImageResource(R.drawable.ic_action_share_2);
                break;
            default:
                timerKnob.setVisibility(View.INVISIBLE);
        }
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
        timerDial = (TimerDial) findViewById(R.id.timerDial);
        timerKnob = (ImageView) findViewById(R.id.timerText);
        timerButtonContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                triggerButtonAction();
            }
        });

        cameraPreviewFriendReplyText = findViewById(R.id.cameraPreviewFriendReplyText);
        messageVideoFriendReplyText = findViewById(R.id.messageVideoFriendReplyText);
        coverAnimation = findViewById(R.id.coverAnimation);

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
        createSurface();

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        setAppState(AppState.Off);
        tearDownSurface();
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

        if (state == AppState.PlaybackOnly)
        {
            abortBeforeRecording();
            return;
        }

        if (state == AppState.Recording)
        {
            stopRecording();
            return;
        }

        if (state == AppState.PreviewReady)
        {
            sendEmail(recordPreviewFile);
            closeResultPreview();
        }
    }

    @Override
    public void onBackPressed()
    {
        AppState state = getAppState();
        if (state == AppState.PreviewReady)
            closeResultPreview();
        else
            super.onBackPressed();
    }

    private void abortBeforeRecording()
    {
        AndroidUtils.isMainThread();
        setAppState(AppState.Transition);
        heartbeatTimer.abort();
//        cameraPreviewView.abortBeforeRecording();
        tearDownSurface();

        createSurface();
    }

    class HeartbeatTimer extends Thread
    {
        private long startRecordingTime;
        private long messageVideoDuration;
        private boolean recordingStarted;
        private long endRecordingTime;
        private long startTime = System.currentTimeMillis();
        private Long pauseStart;
        private AtomicBoolean cancel = new AtomicBoolean(false);

        HeartbeatTimer(long startRecordingTime, long messageVideoDuration, boolean recordingStarted)
        {
            this.startRecordingTime = startRecordingTime;
            this.recordingStarted = recordingStarted;
            this.messageVideoDuration = messageVideoDuration;
            endRecordingTime = messageVideoDuration + RECORDING_TIME;
        }

        public void abort()
        {
            cancel.set(true);
        }

        class UpdateTimerRunnable implements Runnable
        {
            int now;

            @Override
            public void run()
            {
                timerDial.updateOffset((int) now);
            }
        }

        public synchronized void endPause()
        {
            if (pauseStart != null)
            {
                long calcStartAdjust = startTime + (System.currentTimeMillis() - pauseStart);

                startTime = calcStartAdjust > System.currentTimeMillis() ? System.currentTimeMillis() : calcStartAdjust;
                pauseStart = null;
            }
        }

        @Override
        public void run()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    timerDial.resetAnimation((int) startRecordingTime, (int) endRecordingTime);
                }
            });

            UpdateTimerRunnable updateTimerRunnable = new UpdateTimerRunnable();
            while (true)
            {
                try
                {
                    Thread.sleep(20);
                }
                catch (InterruptedException e)
                {
                }

                if (cancel.get())
                    break;

                long now = System.currentTimeMillis() - startTime;

                synchronized (this)
                {
                    if (pauseStart != null)
                        continue;
                }

                updateTimerRunnable.now = (int) now;
                runOnUiThread(updateTimerRunnable);


                if (!recordingStarted && now >= startRecordingTime)
                {
                    recordingStarted = true;
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            pauseStart = System.currentTimeMillis();
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

                    heartbeatTimer = null;
                    break;
                }
            }
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    timerDial.resetAnimation(null, null);
                }
            });
        }
    }

    private void startPlaybackRecording()
    {
        AndroidUtils.isMainThread();
        int recordingStartMillis = chatMessage == null ? 0 : (int) Math.round(chatMessage.metadata.startRecording * 1000);
        if (messageVideoView != null)
        {
            messageVideoThumbnailView.setVisibility(View.GONE);
            messageVideoView.setVisibility(View.VISIBLE);
            setAppState(AppState.PlaybackOnly);
            messageVideoView.seekTo(0);
            messageVideoView.start();
        }

        if (recordingStartMillis == 0)
        {
            startRecording();
        }

        int chatMessageDuration = chatMessageVideoMetadata == null ? 0 : chatMessageVideoMetadata.duration;
        assert heartbeatTimer == null; //Just checking. This would be bad.
        heartbeatTimer = new HeartbeatTimer(recordingStartMillis, chatMessageDuration, recordingStartMillis == 0);
        heartbeatTimer.start();
    }

    private void startRecording()
    {
        AndroidUtils.isMainThread();
        hideMessage(cameraPreviewFriendReplyText);
        if (messageVideoView != null)
            showMessage(messageVideoFriendReplyText);
        setAppState(AppState.RecordingLimbo);
        if (messageVideoView != null)
            messageVideoView.pause();
        cameraPreviewView.startRecording();
    }

    private void stopRecording()
    {
        AndroidUtils.isMainThread();
        setAppState(AppState.PreviewLoading);
        hideMessage(messageVideoFriendReplyText);
        if (heartbeatTimer != null)
            heartbeatTimer.abort();
        cameraPreviewView.stopRecording();
    }

    private void previewSurfaceReady()
    {
        CWLog.i(NewCameraActivity.class, "previewSurfaceReady called");
        initStartState();
    }

    private void initStartState()
    {
        chatMessage = null;
        chatMessageVideoMetadata = null;
        recordPreviewFile = null;

        new MessageLoaderTask().execute();
    }

    private void messageLoaded(ChatMessage message)
    {
        this.chatMessage = message;

        if (chatMessage != null)
        {
            messageVideoView = new DynamicVideoView(NewCameraActivity.this, chatMessageVideoMetadata.videoFile, chatMessageVideoMetadata.width, chatMessageVideoMetadata.height);
            videoViewContainer.addView(messageVideoView);
            messageVideoThumbnailView = new DynamicVideoThumbImageView(NewCameraActivity.this, chatMessageVideoMetadata.width, chatMessageVideoMetadata.height);
            videoViewContainer.addView(messageVideoThumbnailView);
            messageVideoView.setVisibility(View.GONE);
            messageVideoThumbnailView.setVisibility(View.VISIBLE);
            messageVideoThumbnailView.setImageBitmap(chatMessageVideoBitmap);
        }

        CWLog.i(NewCameraActivity.class, "messageLoaded complete");
        liveForRecording();
    }

    private void liveForRecording()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                AppState appStateTest = getAppState();
                CWLog.i(NewCameraActivity.class, "appState: " + appStateTest);
                if (appStateTest == AppState.LoadingFileCamera)
                {
                    if (chatMessage != null && chatMessage.metadata.startRecording > 0d)
                    {
                        showMessage(cameraPreviewFriendReplyText);
                    }
                    setAppState(AppState.ReadyStopped);
                }
            }
        });
    }

    class LoadAndShowVideoMessageTask extends AsyncTask<File, Void, VideoUtils.VideoMetadata>
    {
        LoadAndShowVideoMessageTask()
        {
        }

        @Override
        protected VideoUtils.VideoMetadata doInBackground(File... params)
        {
            try
            {
                return VideoUtils.findMetadata(params[0]);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

        }

        @Override
        protected void onPostExecute(VideoUtils.VideoMetadata videoInfo)
        {
            recordPreviewVideoView = new DynamicVideoView(NewCameraActivity.this, recordPreviewFile, videoInfo.width, videoInfo.height);

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
            setAppState(AppState.PreviewReady);
        }
    }

    private void createSurface()
    {
        AndroidUtils.isMainThread();
        setAppState(AppState.LoadingFileCamera);
        hideMessage(cameraPreviewFriendReplyText);
        hideMessage(messageVideoFriendReplyText);
        CWLog.i(NewCameraActivity.class, "createSurface started");
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
                if (messageVideoThumbnailView != null && messageVideoThumbnailView.getVisibility() == View.VISIBLE)
                {
                    messageVideoThumbnailView.setVisibility(View.GONE);
                    messageVideoView.setVisibility(View.VISIBLE);
                }
                if (messageVideoView != null)
                    messageVideoView.start();

                heartbeatTimer.endPause();
            }

            @Override
            public void recordingDone(File videoFile)
            {
                showResultPreview(videoFile);
            }
        });
        cameraPreviewContainer.addView(cameraPreviewView);
        CWLog.i(NewCameraActivity.class, "createSurface view added");
    }

    private void showResultPreview(File videoFile)
    {
        AndroidUtils.isMainThread();
        this.recordPreviewFile = videoFile;
        tearDownSurface();
        new LoadAndShowVideoMessageTask().execute(recordPreviewFile);
    }

    private void closeResultPreview()
    {
        AndroidUtils.isMainThread();
        recordPreviewFile = null;
        cameraPreviewContainer.removeView(recordPreviewVideoView);
        closeRecordPreviewView.setVisibility(View.GONE);

        createSurface();
    }

    private void tearDownSurface()
    {
        AndroidUtils.isMainThread();
        cameraPreviewContainer.removeAllViews();
        if (cameraPreviewView != null)
        {
            cameraPreviewView.releaseResources();
            cameraPreviewView = null;
        }
        videoViewContainer.removeAllViews();
        messageVideoView = null;
        messageVideoThumbnailView = null;
        chatMessageVideoBitmap = null;
    }

    class MessageLoaderTask extends AsyncTask<Void, Void, ChatMessage>
    {
        @Override
        protected ChatMessage doInBackground(Void... params)
        {
            ChatMessage cm = ShareUtils.extractFileAttachment(NewCameraActivity.this);
            if (cm != null)
            {
                try
                {
                    chatMessageVideoMetadata = VideoUtils.findMetadata(cm.messageVideo);
                    chatMessageVideoBitmap = VideoUtils.createVideoFrame(cm.messageVideo.getPath(), 0l);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
            return cm;
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
                    long chatMessageDuration = chatMessageVideoMetadata == null ? 0 : chatMessageVideoMetadata.duration;
                    sendMessageMetadata.startRecording = ((double) Math.max(chatMessageDuration - startRecordingMillis, 0)) / 1000d;

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

    private void showMessage(View messageView)
    {
        if (messageView.getVisibility() != View.GONE)
            return;

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.message_fade_in);
        /*animation.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {

            }

            @Override
            public void onAnimationEnd(Animation animation)
            {

            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });*/
        messageView.startAnimation(animation);
        messageView.setVisibility(View.VISIBLE);
    }

    private void hideMessage(View messageView)
    {
        if (messageView.getVisibility() != View.VISIBLE)
            return;

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.message_fade_out);
                /*animation.setAnimationListener(new Animation.AnimationListener()
                {
                    @Override
                    public void onAnimationStart(Animation animation)
                    {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation)
                    {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation)
                    {

                    }
                });*/
        messageView.startAnimation(animation);
        messageView.setVisibility(View.GONE);
    }
}
