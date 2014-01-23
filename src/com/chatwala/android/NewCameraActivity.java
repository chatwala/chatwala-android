package com.chatwala.android;

import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.http.GetMessageFileRequest;
import com.chatwala.android.http.PostSubmitMessageRequest;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.superbus.PostSubmitMessageCommand;
import com.chatwala.android.superbus.PutMessageFileCommand;
import com.chatwala.android.superbus.PutUserProfilePictureCommand;
import com.chatwala.android.ui.TimerDial;
import com.chatwala.android.util.*;
import com.j256.ormlite.dao.Dao;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 11/18/13
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewCameraActivity extends BaseNavigationDrawerActivity
{
    public static final int RECORDING_TIME = 10000;
    public static final int VIDEO_PLAYBACK_START_DELAY = 500;
    public static final String HANGOUTS_PACKAGE_NAME = "com.google.android.talk";
    private int openingVolume;
    private Handler buttonDelayHandler;
    private View timerButtonContainer;

    private ChatwalaMessage playbackMessage = null;
    private ChatwalaMessage messageToSendDirectly = null;
    private static final String MESSAGE_ID = "MESSAGE_ID";
    public static final String PENDING_SEND_URL = "PENDING_SEND_URL";
    public static final String OPEN_DRAWER = "OPEN_DRAWER";

    public enum AppState
    {
        Off(true), Transition(true), LoadingFileCamera(true), ReadyStopped(true), PlaybackOnly(false), PlaybackRecording(false), RecordingLimbo(false), Recording(false), PreviewLoading(false), PreviewReady(false), Sharing(false);

        boolean doesStateEnableNavigationDrawer;

        private AppState(boolean drawerEnabled)
        {
            doesStateEnableNavigationDrawer = drawerEnabled;
        }

        public boolean shouldEnableDrawer()
        {
            return doesStateEnableNavigationDrawer;
        }
    }

    // ************* onCreate only *************
    private CroppingLayout cameraPreviewContainer, videoViewContainer;
    private View topFrameMessage;
    private TextView topFrameMessageText;
    private View bottomFrameMessage;
    private TextView bottomFrameMessageText;
    private DynamicTextureVideoView messageVideoView;
    private DynamicTextureVideoView recordPreviewVideoView;
    private ReplayCountingCompletionListener recordPreviewCompletionListener;
    private View closeRecordPreviewView;
    private ImageView timerKnob;
    private TimerDial timerDial;
    private View splash;
    // ************* onCreate only *************

    // ************* DANGEROUS STATE *************
    private CameraPreviewView cameraPreviewView;
    private ChatwalaMessage incomingMessage;
    private VideoUtils.VideoMetadata chatMessageVideoMetadata;
    private File recordPreviewFile = null;
    private AppState appState = AppState.Off;
    private HeartbeatTimer heartbeatTimer;
    private boolean activityActive;
    private long analyticsTimerStart;
    // ************* DANGEROUS STATE *************

    private boolean closePreviewOnReturn = false;

    public synchronized AppState getAppState()
    {
        return appState;
    }

    public synchronized void setAppState(AppState appState)
    {
        setAppState(appState, false);
    }

    public synchronized void setAppState(AppState appState, boolean buttonPress)
    {
        CWLog.b(NewCameraActivity.class, "setAppState: " + appState + " (" + System.currentTimeMillis() + ")");
        AndroidUtils.isMainThread();

        analyticsStateEnd(appState, buttonPress);

        this.appState = appState;
        toggleDrawerEnabled(this.appState.shouldEnableDrawer());

        delayButtonPress();

        switch (this.appState)
        {
            case ReadyStopped:
                timerKnob.setVisibility(View.VISIBLE);
                if (incomingMessage != null)
                    timerKnob.setImageResource(R.drawable.ic_action_playback_play);
                else
                    timerKnob.setImageResource(R.drawable.record_circle);
                break;
            case PlaybackOnly:
                analyticsTimerReset();
                CWAnalytics.sendStartReviewEvent();
                setTimerKnobForRecording();
                break;
            case PlaybackRecording:
                analyticsTimerReset();
                CWAnalytics.sendStartReactionEvent();
                setTimerKnobForRecording();
                break;
            case Recording:
                analyticsTimerReset();
                CWAnalytics.sendRecordingStartEvent(true);
                setTimerKnobForRecording();
                break;
            case PreviewReady:
                timerKnob.setVisibility(View.VISIBLE);
                timerKnob.setImageResource(R.drawable.ic_action_send_ios);
                break;
            case Sharing:
                closeRecordPreviewView.setVisibility(View.GONE);
                break;
            default:
                timerKnob.setVisibility(View.INVISIBLE);
        }
    }

    private void analyticsTimerReset()
    {
        analyticsTimerStart = System.currentTimeMillis();
    }

    private void analyticsStateEnd(AppState appState, boolean buttonPress)
    {
        if (appState != AppState.Transition)
        {
            long duration = analyticsDuration();
            switch (this.appState)
            {
                case PlaybackOnly:
                    CWAnalytics.sendReviewCompleteEvent(duration);
                    break;
                case PlaybackRecording:
                    CWAnalytics.sendReactionCompleteEvent(duration);
                    break;
                case Recording:
                    CWAnalytics.sendRecordingEndEvent(buttonPress, duration);
                    break;

            }
        }
    }

    private long analyticsDuration()
    {
        return System.currentTimeMillis() - analyticsTimerStart;
    }

    private void setTimerKnobForRecording()
    {
        timerKnob.setVisibility(View.VISIBLE);
        timerKnob.setImageResource(R.drawable.record_stop);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        CWAnalytics.initAnalytics(NewCameraActivity.this, !replyMessageAvailable());
        CWLog.b(NewCameraActivity.class, "onCreate start");

        buttonDelayHandler = new Handler();

        setMainContent(getLayoutInflater().inflate(R.layout.activity_main, (ViewGroup) getWindow().getDecorView(), false));

        ChatwalaApplication application = (ChatwalaApplication) getApplication();

        cameraPreviewContainer = (CroppingLayout) findViewById(R.id.surface_view_container);
        videoViewContainer = (CroppingLayout) findViewById(R.id.video_view_container);
        timerButtonContainer = findViewById(R.id.timerContainer);
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

        topFrameMessage = findViewById(R.id.topFrameMessage);
        topFrameMessageText = (TextView) findViewById(R.id.topFrameMessageText);
        Typeface fontDemi = ((ChatwalaApplication) getApplication()).fontMd;
        topFrameMessageText.setTypeface(fontDemi);
        bottomFrameMessage = findViewById(R.id.bottomFrameMessage);
        bottomFrameMessageText = (TextView) findViewById(R.id.bottomFrameMessageText);
        bottomFrameMessageText.setTypeface(fontDemi);

        closeRecordPreviewView = findViewById(R.id.closeVideoPreview);
        closeRecordPreviewView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                CWLog.userAction(NewCameraActivity.class, "Close record preview pressed in state: " + getAppState().name());
                CWAnalytics.sendRedoMessageEvent((long) recordPreviewCompletionListener.replays);
                closeResultPreview();
            }
        });
        CWLog.b(NewCameraActivity.class, "onCreate end");

        if(savedInstanceState != null && savedInstanceState.containsKey(PENDING_SEND_URL))
        {
            recordPreviewFile = new File(savedInstanceState.getString(PENDING_SEND_URL));
        }

        if(getIntent().hasExtra(OPEN_DRAWER))
        {
            openDrawer();
        }
//        captureOpeningVolume();
    }

    private void runWaterSplash()
    {
        if (splash != null)
        {
            final ViewGroup root = findViewRoot();
            root.removeView(splash);
        }

        splash = getLayoutInflater().inflate(R.layout.splash_ripple, null);
        final ViewGroup root = findViewRoot();
        root.addView(splash);
        CWLog.b(NewCameraActivity.class, "runWaterSplash end");
    }

    private void removeWaterSplash()
    {
        if (splash != null)
        {
            final ViewGroup root = findViewRoot();
            Animation animation = AnimationUtils.loadAnimation(NewCameraActivity.this, R.anim.splash_fade_out);

            final View innerSplash = splash;
            animation.setAnimationListener(new Animation.AnimationListener()
            {
                @Override
                public void onAnimationStart(Animation animation)
                {

                }

                @Override
                public void onAnimationEnd(Animation animation)
                {
                    root.removeView(innerSplash);
                }

                @Override
                public void onAnimationRepeat(Animation animation)
                {

                }
            });

            splash.startAnimation(animation);

            splash = null;
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        CWLog.b(NewCameraActivity.class, "onResume");

        activityActive = true;
        setAppState(AppState.Transition);

//        setReviewVolume();

        if (closePreviewOnReturn)
        {
            closePreviewOnReturn = false;
            closeResultPreview();
        }
        else if (hasPendingSendMessage())
        {
            if(!replyMessageAvailable())
            {
                prepManualSend();
            }
            setAppState(AppState.PreviewLoading);
            if(recordPreviewFile == null)
            {
                ChatwalaMessage savedMessage = ShareUtils.extractFileAttachment(NewCameraActivity.this, getIntent().getStringExtra(PENDING_SEND_URL));
                getIntent().removeExtra(PENDING_SEND_URL);
                recordPreviewFile = savedMessage.getMessageFile();
            }
            showResultPreview(recordPreviewFile);
        }
        else
        {
            //Kick off attachment load
            createSurface();
        }
    }

    @Override
    protected void performAddButtonAction()
    {
        if(replyMessageAvailable())
        {
            NewCameraActivity.startMe(NewCameraActivity.this);
            finish();
        }
        else
        {
            closeDrawer();
        }
    }

    @Override
    protected void onPause()
    {
        if (heartbeatTimer != null)
            heartbeatTimer.abort();

        super.onPause();

        activityActive = false;

        AppState state = getAppState();

        if (state == AppState.PlaybackRecording || state == AppState.Recording)
        {
            cameraPreviewView.abortRecording();
        }

        setAppState(AppState.Off);

//        resetOpeningVolume();
        tearDownSurface();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if(getAppState() == AppState.PreviewReady)
        {
            outState.putString(PENDING_SEND_URL, recordPreviewFile.getAbsolutePath());
        }
    }

    public boolean isActivityActive()
    {
        return activityActive;
    }

    private void delayButtonPress()
    {
        switch (getAppState())
        {
            case PlaybackOnly:
            case PlaybackRecording:
            case Recording:
//            case PreviewLoading:
                timerButtonContainer.setEnabled(false);
                buttonDelayHandler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        timerButtonContainer.setEnabled(true);
                    }
                }, 1000);
        }
    }

    private void triggerButtonAction()
    {
        AppState state = getAppState();
        CWLog.userAction(NewCameraActivity.class, "Timer button pressed in state: " + state.name());

        //Don't do anything.  These should be very short states.
        if (state == AppState.Off || state == AppState.Transition || state == AppState.LoadingFileCamera || state == AppState.RecordingLimbo || state == AppState.PreviewLoading || state == AppState.Sharing)
            return;

        if (state == AppState.ReadyStopped)
        {
            startPlaybackRecording();
            return;
        }

        if (state == AppState.PlaybackOnly)
        {
            CWAnalytics.sendStopReviewEvent(analyticsDuration());
            abortBeforeRecording();
            return;
        }

        if (state == AppState.PlaybackRecording)
        {
            CWAnalytics.sendStopReactionEvent(analyticsDuration());
            abortRecording();
            return;
        }

        if (state == AppState.Recording)
        {
            stopRecording(true);
            return;
        }

        if (state == AppState.PreviewReady)
        {
            setAppState(AppState.Sharing);
            CWAnalytics.sendSendMessageEvent((long) recordPreviewCompletionListener.replays);

            new AsyncTask<Void, Void, Boolean>()
            {
                @Override
                protected Boolean doInBackground(Void... params)
                {
                    if(replyMessageAvailable() && playbackMessage == null)
                    {
                        try
                        {
                            playbackMessage = DatabaseHelper.getInstance(NewCameraActivity.this).getChatwalaMessageDao().queryForId(getIntent().getStringExtra(MESSAGE_ID));
                        }
                        catch (Exception e)
                        {
                            CWLog.softExceptionLog(NewCameraActivity.class, "Error loading playback for share", e);
                            //On any exception, just continue on without it.  We probably had a problem with the initial video load and the user started a new conversation
                            playbackMessage = null;
                        }
                    }

                    if (playbackMessage == null && messageToSendDirectly == null)
                    {
                        File outFile = ZipUtil.buildZipToSend(NewCameraActivity.this, recordPreviewFile, incomingMessage, chatMessageVideoMetadata, null);
                        ChatwalaNotificationManager.makeErrorInitialSendNotification(NewCameraActivity.this, outFile);
                        return false;
                    }
                    else
                    {
                        if (playbackMessage == null || playbackMessage.getSenderId().startsWith("unknown"))
                        {
                            final String messageId = messageToSendDirectly.getMessageId();
                            messageToSendDirectly = null;

                            final File outFile = ZipUtil.buildZipToSend(NewCameraActivity.this, recordPreviewFile, incomingMessage, chatMessageVideoMetadata, messageId);

                            DataProcessor.runProcess(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    BusHelper.submitCommandSync(NewCameraActivity.this, new PutMessageFileCommand(outFile.getAbsolutePath(), messageId, null));
                                }
                            });

                            if (AppPrefs.getInstance(NewCameraActivity.this).getPrefUseSms())
                            {
                                sendSms(messageId);
                            }
                            else
                            {
                                sendEmail(messageId);
                            }

                            return null;
                        }
                        else
                        {
                            final File outboxVideoFile = MessageDataStore.makeOutboxVideoFile();
                            try
                            {
                                FileOutputStream outStream = new FileOutputStream(outboxVideoFile);

                                FileInputStream inStream = new FileInputStream(recordPreviewFile);

                                IOUtils.copy(inStream, outStream);

                                outStream.close();
                                inStream.close();

                                recordPreviewFile.delete();
                            }
                            catch (FileNotFoundException e)
                            {
                                throw new RuntimeException(e);
                            }
                            catch (IOException e)
                            {
                                throw new RuntimeException(e);
                            }


                            DataProcessor.runProcess(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    BusHelper.submitCommandSync(NewCameraActivity.this, new PostSubmitMessageCommand(outboxVideoFile.getPath(), playbackMessage.getSenderId(), playbackMessage.getMessageId(), chatMessageVideoMetadata));
                                }
                            });

                            return true;
                        }
                    }
                }

                @Override
                protected void onPostExecute(final Boolean complete)
                {
                    if(complete != null)
                    {
                        if(complete)
                        {
                            Toast.makeText(NewCameraActivity.this, "Message sent.", Toast.LENGTH_LONG).show();

                            AppPrefs prefs = AppPrefs.getInstance(NewCameraActivity.this);
                            boolean showFeedback = false;
                            if(!prefs.getPrefFeedbackShown())
                            {
                                showFeedback = prefs.recordMessageSent();
                            }

                            if(!prefs.isImageReviewed() && MessageDataStore.findUserImageInLocalStore(prefs.getUserId()).exists())
                            {
                                UpdateProfilePicActivity.startMe(NewCameraActivity.this, true);
                            }
                            else if(showFeedback)
                            {
                                FeedbackActivity.startMe(NewCameraActivity.this, true);
                            }
                            else
                            {
                                NewCameraActivity.startMe(NewCameraActivity.this);
                            }

                            finish();
                        }
                        else
                        {
                            Toast.makeText(NewCameraActivity.this, "Couldn't contact server.  Please try again later.", Toast.LENGTH_LONG).show();
                            NewCameraActivity.startMe(NewCameraActivity.this);
                            finish();
                        }
                    }
                }
            }.execute();
        }
    }

    private void abortRecording()
    {
        CWLog.b(NewCameraActivity.class, "abortRecording");
        cameraPreviewView.abortRecording();
        abortBeforeRecording();
    }

    @Override
    public void onBackPressed()
    {
        CWLog.userAction(NewCameraActivity.class, "onBackPressed");
        AppState state = getAppState();
        if (state == AppState.PreviewReady)
            closeResultPreview();
        else
            super.onBackPressed();
    }

    private void abortBeforeRecording()
    {
        CWLog.b(NewCameraActivity.class, "abortBeforeRecording");
        AndroidUtils.isMainThread();
        setAppState(AppState.Transition);
        heartbeatTimer.abort();
        tearDownSurface();

        createSurface();
    }

    class HeartbeatTimer extends Thread
    {
        private final long startRecordingTime;
        private long messageVideoDuration;
        private boolean recordingStarted;
        private long endRecordingTime;
        private long startTime = System.currentTimeMillis();
        private Long pauseStart;
        private AtomicBoolean cancel = new AtomicBoolean(false);

        HeartbeatTimer(long startRecordingTime, long messageVideoDuration, boolean recordingStarted)
        {
            this.startRecordingTime = startRecordingTime > 0 ? startRecordingTime + VIDEO_PLAYBACK_START_DELAY : startRecordingTime;
            this.recordingStarted = recordingStarted;
            this.messageVideoDuration = messageVideoDuration;
            endRecordingTime = messageVideoDuration + RECORDING_TIME;
        }

        public void abort()
        {
            CWLog.b(NewCameraActivity.class, "HeartbeatTimer:abort");
            cancel.set(true);
        }

        public boolean isAborted()
        {
            return cancel.get();
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
            CWLog.b(NewCameraActivity.class, "HeartbeatTimer:endPause");
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
                    CWLog.b(NewCameraActivity.class, "HeartbeatTimer: inside start recording block");
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
                    CWLog.b(NewCameraActivity.class, "stop HeartbeatTimer");
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            stopRecording(false);
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
        CWLog.b(NewCameraActivity.class, "startPlaybackRecording");
        AndroidUtils.isMainThread();

        int recordingStartMillis = incomingMessage == null ? 0 : (int) Math.round(incomingMessage.getStartRecording() * 1000);
        if (messageVideoView != null)
        {
            setAppState(AppState.PlaybackOnly);
            messageVideoView.start();
            topFrameMessageText.setVisibility(View.GONE);
        }
        else
        {
            hideMessage(topFrameMessage);
        }

        boolean shouldStartRecordingNow = recordingStartMillis == 0;
        if (shouldStartRecordingNow)
        {
            startRecording();
        }

        int chatMessageDuration = chatMessageVideoMetadata == null ? 0 : chatMessageVideoMetadata.duration;
        assert heartbeatTimer == null; //Just checking. This would be bad.

        heartbeatTimer = new HeartbeatTimer(recordingStartMillis, chatMessageDuration, shouldStartRecordingNow);
        heartbeatTimer.start();
    }

    private void startRecording()
    {
        CWLog.b(NewCameraActivity.class, "startRecording");
        AndroidUtils.isMainThread();

        if (incomingMessage == null)
            hideMessage(bottomFrameMessage);

        setAppState(AppState.RecordingLimbo);
        if (messageVideoView != null)
            messageVideoView.pause();
        cameraPreviewView.startRecording();
    }

    private void stopRecording(boolean buttonPress)
    {
        CWLog.b(NewCameraActivity.class, "stopRecording");
        AndroidUtils.isMainThread();
        setAppState(AppState.PreviewLoading, buttonPress);
        hideMessage(bottomFrameMessage);
        if (heartbeatTimer != null)
            heartbeatTimer.abort();
        cameraPreviewView.stopRecording();
    }

    private void previewSurfaceReady()
    {
        CWLog.b(NewCameraActivity.class, "previewSurfaceReady");
        initStartState();
    }

    private void initStartState()
    {
        CWLog.b(NewCameraActivity.class, "initStartState");
        incomingMessage = null;
        chatMessageVideoMetadata = null;
        recordPreviewFile = null;

        if (replyMessageAvailable())
        {
            new MessageLoaderTask().execute();
        }
        else
        {
            messageLoaded(null);
        }
    }

    private void messageLoaded(ChatwalaMessage message)
    {
        CWLog.b(NewCameraActivity.class, "messageLoaded");
        incomingMessage = message;

        if (incomingMessage != null)
        {
            messageVideoView = new DynamicTextureVideoView(NewCameraActivity.this, chatMessageVideoMetadata.videoFile, chatMessageVideoMetadata.width, chatMessageVideoMetadata.height, chatMessageVideoMetadata.rotation, new DynamicTextureVideoView.VideoReadyCallback()
            {
                @Override
                public void ready()
                {
                    removeWaterSplash();
                }
            }, true);
            videoViewContainer.addView(messageVideoView);
            messageVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    setAppState(AppState.Recording);
                    showMessage(bottomFrameMessage, bottomFrameMessageText, R.color.message_background_alpha, R.string.now_record_reply);
                }
            });
            showMessage(topFrameMessage, topFrameMessageText, R.color.message_background_alpha, R.string.play_message_record_reaction);
        }
        else
        {
            removeWaterSplash();
            showMessage(bottomFrameMessage, bottomFrameMessageText, R.color.message_background_clear, R.string.basic_instructions);
        }

        liveForRecording();
    }

    private void liveForRecording()
    {
        CWLog.b(NewCameraActivity.class, "liveForRecording");
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                AppState appStateTest = getAppState();
                CWLog.i(NewCameraActivity.class, "appState: " + appStateTest);
                if (appStateTest == AppState.LoadingFileCamera)
                {
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
            recordPreviewVideoView = new DynamicTextureVideoView(NewCameraActivity.this, recordPreviewFile, videoInfo.width, videoInfo.height, videoInfo.rotation, null, false);

            cameraPreviewContainer.addView(recordPreviewVideoView);
            closeRecordPreviewView.setVisibility(View.VISIBLE);
            recordPreviewVideoView.start();
            recordPreviewCompletionListener = new ReplayCountingCompletionListener();
            recordPreviewVideoView.setOnCompletionListener(recordPreviewCompletionListener);

            setAppState(AppState.PreviewReady);
        }
    }

    private boolean replyMessageAvailable()
    {
        return getIntent().hasExtra(MESSAGE_ID) || getIntent().getData() != null;
    }

    private boolean hasPendingSendMessage()
    {
        return getIntent().hasExtra(PENDING_SEND_URL) || recordPreviewFile != null;
    }

    private void createSurface()
    {
        AndroidUtils.isMainThread();

        if (replyMessageAvailable())
        {
            runWaterSplash();
        }
        else
        {
            prepManualSend();
        }

        CWLog.b(NewCameraActivity.class, "createSurface");
        setAppState(AppState.LoadingFileCamera);
        hideMessage(topFrameMessage);
        hideMessage(bottomFrameMessage);
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
                if (incomingMessage == null)
                    setAppState(AppState.Recording);
                else
                    setAppState(AppState.PlaybackRecording);
                if (messageVideoView != null)
                {
                    hideMessage(topFrameMessage);
                    new Handler().postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                messageVideoView.start();
                            }
                            catch (Exception e)
                            {
                                //Whoops
                            }
                        }
                    }, VIDEO_PLAYBACK_START_DELAY);
                }

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
        CWLog.b(NewCameraActivity.class, "showResultPreview");
        AndroidUtils.isMainThread();
        this.recordPreviewFile = videoFile;
        tearDownSurface();
        new LoadAndShowVideoMessageTask().execute(recordPreviewFile);
    }

    private void closeResultPreview()
    {
        CWLog.b(NewCameraActivity.class, "showResultPreview");
        AndroidUtils.isMainThread();
        recordPreviewFile = null;
        recordPreviewVideoView.setOnCompletionListener(null);
        recordPreviewVideoView.pause();
        cameraPreviewContainer.removeView(recordPreviewVideoView);
        findViewById(R.id.recordPreviewClick).setOnClickListener(null);
        closeRecordPreviewView.setVisibility(View.GONE);

        createSurface();
    }

    private void tearDownSurface()
    {
        CWLog.b(NewCameraActivity.class, "tearDownSurface: start");
        AndroidUtils.isMainThread();
        if (heartbeatTimer != null)
            heartbeatTimer.abort();
        if (messageVideoView != null)
            messageVideoView.pause();
        if (recordPreviewVideoView != null)
            recordPreviewVideoView.pause();

        cameraPreviewContainer.removeAllViews();
        if (cameraPreviewView != null)
        {
            cameraPreviewView.releaseResources();
            cameraPreviewView = null;
        }
        videoViewContainer.removeAllViews();
        messageVideoView = null;
        findViewById(R.id.recordPreviewClick).setOnClickListener(null);
        CWLog.b(NewCameraActivity.class, "tearDownSurface: end");
    }

    class MessageLoaderTask extends AsyncTask<Void, Void, ChatwalaMessage>
    {
        @Override
        protected ChatwalaMessage doInBackground(Void... params)
        {
            try
            {
                String playbackMessageId;
                if (getIntent().hasExtra(MESSAGE_ID))
                {
                    playbackMessageId = getIntent().getStringExtra(MESSAGE_ID);
                }
                else
                {
                    playbackMessageId = ShareUtils.getIdFromIntent(getIntent());
                }

                Dao<ChatwalaMessage, String> messageDao = DatabaseHelper.getInstance(NewCameraActivity.this).getChatwalaMessageDao();
                playbackMessage = messageDao.queryForId(playbackMessageId);

                if(playbackMessage == null || playbackMessage.getMessageFile() == null)
                {
                    Log.d("##########", "Refetching message: " + playbackMessageId);
                    playbackMessage = new ChatwalaMessage();
                    playbackMessage.setMessageId(playbackMessageId);
                    playbackMessage = (ChatwalaMessage)new GetMessageFileRequest(NewCameraActivity.this, playbackMessage).execute();
                }
                chatMessageVideoMetadata = VideoUtils.findMetadata(playbackMessage.getMessageFile());

                if(playbackMessage.getMessageState() == ChatwalaMessage.MessageState.UNREAD)
                {
                    playbackMessage.setMessageState(ChatwalaMessage.MessageState.READ);
                    messageDao.update(playbackMessage);
                    BroadcastSender.makeNewMessagesBroadcast(NewCameraActivity.this);
                }

                return playbackMessage;
            }
            catch (TransientException e)
            {
                chatMessageVideoMetadata = null;
                playbackMessage = null;
                return null;
            }
            catch (PermanentException e)
            {
                chatMessageVideoMetadata = null;
                playbackMessage = null;
                return null;
            }
            catch (IOException e)
            {
                chatMessageVideoMetadata = null;
                playbackMessage = null;
                return null;
            }
            catch (SQLException e)
            {
                chatMessageVideoMetadata = null;
                playbackMessage = null;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ChatwalaMessage chatMessage)
        {
            if(chatMessage == null)
            {
                Toast.makeText(NewCameraActivity.this, "Unable to load message. Please try again.", Toast.LENGTH_LONG).show();
                NewCameraActivity.startMe(NewCameraActivity.this);
                finish();
            }
            messageLoaded(chatMessage);
        }
    }

    private void prepManualSend()
    {
        DataProcessor.runProcess(new Runnable()
        {
            @Override
            public void run()
            {
                int attempts = 0;
                while (attempts < 3)
                {
                    try
                    {
                        ChatwalaMessage messageInfo = new PostSubmitMessageRequest(NewCameraActivity.this, null, null, null, null).execute();
                        if (messageInfo != null)
                        {
                            messageToSendDirectly = messageInfo;
                            break;
                        }
                    }
                    catch (TransientException e)
                    {
                        CWLog.softExceptionLog(NewCameraActivity.class, "Couldn't get message id", e);
                    }
                    catch (PermanentException e)
                    {
                        CWLog.softExceptionLog(NewCameraActivity.class, "Couldn't get message id", e);
                    }
                    attempts++;
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void sendEmail(final String messageId)
    {
        Log.d("######### SENDING MESSAGE ID: ", messageId);
        String uriText = "mailto:";

        Uri mailtoUri = Uri.parse(uriText);
        //String messageLink = "<a href=\"http://chatwala.com/?" + messageId + "\">View the message</a>.";
        String messageLink = "http://chatwala.com/?" + messageId;

        boolean gmailOk = false;

        Intent gmailIntent = new Intent();
        gmailIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
        gmailIntent.setData(mailtoUri);
        gmailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.message_subject));
        //gmailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Chatwala is a new way to have real conversations with friends. " + messageLink));
        gmailIntent.putExtra(Intent.EXTRA_TEXT, "Chatwala is a new way to have real conversations with friends. View the message:\n\n" + messageLink);

        try
        {
            closePreviewOnReturn = true;
            startActivity(gmailIntent);
            gmailOk = true;
        }
        catch (ActivityNotFoundException ex)
        {
            CWLog.softExceptionLog(NewCameraActivity.class, "Couldn't send gmail", ex);
        }

        if (!gmailOk)
        {
            Intent intent = new Intent(Intent.ACTION_SENDTO);

            intent.setData(mailtoUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.message_subject));
            //intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Chatwala is a new way to have real conversations with friends. " + messageLink));
            intent.putExtra(Intent.EXTRA_TEXT, "Chatwala is a new way to have real conversations with friends. View the message:\n\n" + messageLink);

            startActivity(Intent.createChooser(intent, "Send email..."));
        }
    }

    private void sendSms(final String messageId)
    {
        String messageLink = "http://chatwala.com/?" + messageId;
        String smsText = "Hey, I sent you a video message on Chatwala: " + messageLink;
        closePreviewOnReturn = true;
        openSmsShare(smsText);
    }

    private void openSmsShare(String smsText)
    {
        if (Build.VERSION.SDK_INT > 18)
        {
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(this);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, smsText);

            if (defaultSmsPackageName != null)//Can be null in case that there is no default, then the user would be able to choose any app that support this intent.
            {
                sendIntent.setPackage(defaultSmsPackageName);
            }
            startActivity(sendIntent);
        }
        else
        {
            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            sendIntent.setData(Uri.parse("sms:"));
            sendIntent.putExtra("sms_body", smsText);

            PackageManager pm = getPackageManager();
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(sendIntent, 0);

            ResolveInfo resolveInfo = null;
            if(resolveInfos.size() == 1)
            {
                resolveInfo = resolveInfos.get(0);
            }
            else if(resolveInfos.size() > 1)
            {
                for (ResolveInfo info : resolveInfos)
                {
                    if(info.isDefault)
                    {
                        resolveInfo = info;
                        break;
                    }
                }
                if(resolveInfo == null)
                {
                    List<ResolveInfo> trimApps = new ArrayList<ResolveInfo>(resolveInfos.size());
                    for (ResolveInfo info : resolveInfos)
                    {
                        String packageName = info.activityInfo.applicationInfo.packageName;
                        if(!packageName.equalsIgnoreCase(HANGOUTS_PACKAGE_NAME))
                        {
                            trimApps.add(info);
                        }
                    }
                    if(trimApps.size() == 1)
                        resolveInfo = trimApps.get(0);
                }
            }

            if(resolveInfo != null)
            {
                ActivityInfo activity = resolveInfo.activityInfo;
                ComponentName name = new ComponentName(activity.applicationInfo.packageName, activity.name);
                sendIntent.setComponent(name);
            }

            startActivity(sendIntent);
        }
    }

    private void showMessage(View messageView, TextView messageViewText, int colorRes, int messageRes)
    {
        if (messageView.getVisibility() != View.GONE)
            return;

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.message_fade_in);

        messageView.startAnimation(animation);
        messageView.setBackgroundColor(getResources().getColor(colorRes));
        messageView.setVisibility(View.VISIBLE);
        messageViewText.setText(messageRes);
        messageViewText.setVisibility(View.VISIBLE);
    }

    private void hideMessage(View messageView)
    {
        if (messageView.getVisibility() != View.VISIBLE)
            return;

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.message_fade_out);
        messageView.startAnimation(animation);
        messageView.setVisibility(View.GONE);
    }

    public class ReplayCountingCompletionListener implements MediaPlayer.OnCompletionListener
    {
        public int replays = 0;

        @Override
        public void onCompletion(MediaPlayer mp)
        {
            recordPreviewVideoView.seekTo(0);
            replays++;
            recordPreviewVideoView.start();
        }
    }

    /*private void captureOpeningVolume()
        {
            AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
            openingVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        private void setReviewVolume()
        {
            AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
            int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            long newVolume = Math.round((float) streamMaxVolume * .5);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)newVolume, 0);
        }

        private void resetOpeningVolume()
        {
            AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, openingVolume, 0);
        }*/

    public static void startMe(Context context)
    {
        context.startActivity(new Intent(context, NewCameraActivity.class));
    }

    public static void startMeWithId(Context context, String messageId)
    {
        Intent intent = new Intent(context, NewCameraActivity.class);
        intent.putExtra(MESSAGE_ID, messageId);
        context.startActivity(intent);
    }
}
