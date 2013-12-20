package com.chatwala.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.http.GetMessageFileRequest;
import com.chatwala.android.http.PostSubmitMessageRequest;
import com.chatwala.android.ui.TimerDial;
import com.chatwala.android.util.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

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
    private int openingVolume;
    private Handler buttonDelayHandler;
    private View timerButtonContainer;

    private ChatwalaMessage playbackMessage = null;
    private static final String MESSAGE_ID = "MESSAGE_ID";

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
    private ViewGroup blueMessageDialag;
    private View splash;
    // ************* onCreate only *************

    // ************* DANGEROUS STATE *************
    private CameraPreviewView cameraPreviewView;
    private ChatMessage chatMessage;
    private VideoUtils.VideoMetadata chatMessageVideoMetadata;
    private File recordPreviewFile;
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
                if (chatMessage != null)
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
                //Do nothing
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
        if(appState != AppState.Transition)
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

    class DialagButton
    {
        View.OnClickListener listener;
        int stringRes;

        DialagButton(View.OnClickListener listener, int stringRes)
        {
            this.listener = listener;
            this.stringRes = stringRes;
        }
    }

    private void showDialag(int messageRes, DialagButton... buttons)
    {
        hideDialag();

        blueMessageDialag = (ViewGroup) getLayoutInflater().inflate(R.layout.message_dialag, null);

        blueMessageDialag.findViewById(R.id.messageClose).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                hideDialag();
            }
        });
        Typeface fontMd = ((ChatwalaApplication) getApplication()).fontMd;
        ((TextView) blueMessageDialag.findViewById(R.id.feedbackTitle)).setTypeface(fontMd);
        TextView messageText = (TextView) blueMessageDialag.findViewById(R.id.message_dialag_text);
        TextView messageFiller = (TextView) blueMessageDialag.findViewById(R.id.message_dialag_filler);

        messageText.setTypeface(fontMd);
        messageText.setText(messageRes);

        messageFiller.setTypeface(fontMd);
        messageFiller.setText(messageRes);

        ViewGroup buttonLayout = (ViewGroup) blueMessageDialag.findViewById(R.id.messageDialagButtonContainer);
        for (DialagButton button : buttons)
        {
            View buttonView = makeDialagButton(buttonLayout, button);
        }

        findViewRoot().addView(blueMessageDialag);
    }

    private void hideDialag()
    {
        if (blueMessageDialag != null)
        {
            findViewRoot().removeView(blueMessageDialag);
            blueMessageDialag = null;
        }
    }

    private View makeDialagButton(ViewGroup parent, DialagButton buttonDef)
    {
        View button = getLayoutInflater().inflate(R.layout.message_dialag_button, null);

        Button buttonText = (Button) button.findViewById(R.id.buttonText);
        buttonText.setTypeface(((ChatwalaApplication) getApplication()).fontDemi);
        buttonText.setText(buttonDef.stringRes);
        parent.addView(button);
        buttonText.setOnClickListener(buttonDef.listener);

        return button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        CWAnalytics.initAnalytics(NewCameraActivity.this, !replyMessageAvailable());
        CWLog.b(NewCameraActivity.class, "onCreate start");

        buttonDelayHandler = new Handler();

        setMainContent(getLayoutInflater().inflate(R.layout.crop_test, (ViewGroup) getWindow().getDecorView(), false));
        //setContentView(R.layout.crop_test);

        ChatwalaApplication application = (ChatwalaApplication) getApplication();
        if (!application.isSplashRan())
        {
//            runWaterSplash(application);
        }

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

        timerButtonContainer.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                final View bitDepthView = getLayoutInflater().inflate(R.layout.bit_depth_dialog, null);
                int prefBitDepth = AppPrefs.getInstance(NewCameraActivity.this).getPrefBitDepth();
                ((EditText) bitDepthView.findViewById(R.id.bitDepth)).setText(Integer.toString(prefBitDepth));
                new AlertDialog.Builder(NewCameraActivity.this)
                        .setView(bitDepthView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String bitDepth = ((EditText) bitDepthView.findViewById(R.id.bitDepth)).getText().toString();
                                int bd = Integer.parseInt(bitDepth);
                                if (bd > 10000)
                                {
                                    AppPrefs.getInstance(NewCameraActivity.this).setPrefBitDepth(bd);
                                    dialog.dismiss();
                                    tearDownSurface();
                                    createSurface();
                                }
                                else
                                {
                                    Toast.makeText(NewCameraActivity.this, "Bit depth must be greater than 10000", Toast.LENGTH_LONG).show();
                                }
                            }
                        }).show();
                return false;
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
                CWAnalytics.sendRedoMessageEvent((long)recordPreviewCompletionListener.replays);
                closeResultPreview();
            }
        });
        CWLog.b(NewCameraActivity.class, "onCreate end");

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

    private ViewGroup findViewRoot()
    {
        return (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
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
        else
        {
            //Kick off attachment load
            createSurface();
        }
    }

    @Override
    protected void onPause()
    {
        if(heartbeatTimer != null)
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
            CWAnalytics.sendSendMessageEvent((long)recordPreviewCompletionListener.replays);
            if(playbackMessage == null)
            {
                prepareEmail(recordPreviewFile, chatMessage, chatMessageVideoMetadata);
            }
            else
            {
                new AsyncTask<Void, Void, String>()
                {
                    @Override
                    protected String doInBackground(Void... params)
                    {
                        File outFile = buildZipToSend(recordPreviewFile, chatMessage, chatMessageVideoMetadata);

                        try
                        {
                            return (String) new PostSubmitMessageRequest(NewCameraActivity.this, outFile.getAbsolutePath(), playbackMessage.getSenderId()).execute();
                        }
                        catch (TransientException e)
                        {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            return null;
                        }
                        catch (PermanentException e)
                        {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(String messageId)
                    {
                        Toast.makeText(NewCameraActivity.this, "Message Sent", Toast.LENGTH_LONG).show();
                        playbackMessage = null;
                        createSurface();
                    }
                }.execute();
            }
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

        int recordingStartMillis = chatMessage == null ? 0 : (int) Math.round(chatMessage.metadata.startRecording * 1000);
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

        if (chatMessage == null)
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
        chatMessage = null;
        chatMessageVideoMetadata = null;
        recordPreviewFile = null;

        if(replyMessageAvailable())
        {
            new MessageLoaderTask().execute();
        }
        else
        {
            messageLoaded(null);
        }
    }

    private void messageLoaded(ChatMessage message)
    {
        CWLog.b(NewCameraActivity.class, "messageLoaded");
        this.chatMessage = message;

        if (chatMessage != null)
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

    private void createSurface()
    {
        AndroidUtils.isMainThread();
        if (replyMessageAvailable())
        {
            runWaterSplash();
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
                if(chatMessage == null)
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

    class MessageLoaderTask extends AsyncTask<Void, Void, ChatMessage>
    {
        @Override
        protected ChatMessage doInBackground(Void... params)
        {
            try
            {
                String playbackMessageId;
                if(getIntent().hasExtra(MESSAGE_ID))
                {
                    playbackMessageId = getIntent().getStringExtra(MESSAGE_ID);
                }
                else
                {
                    playbackMessageId = ShareUtils.getIdFromIntent(getIntent());
                }

                ChatMessage toReturn = (ChatMessage)new GetMessageFileRequest(NewCameraActivity.this, playbackMessageId).execute();
                chatMessageVideoMetadata = VideoUtils.findMetadata(toReturn.messageVideo);
                playbackMessage = DatabaseHelper.getInstance(NewCameraActivity.this).getChatwalaMessageDao().queryForId(playbackMessageId);
                return toReturn;
            } catch (TransientException e)
            {
                throw new RuntimeException(e);
            } catch (PermanentException e)
            {
                throw new RuntimeException(e);
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
//            ChatMessage cm = ShareUtils.extractFileAttachment(NewCameraActivity.this);
//            if (cm != null)
//            {
//                try
//                {
//                    chatMessageVideoMetadata = VideoUtils.findMetadata(cm.messageVideo);
////                    chatMessageVideoBitmap = VideoUtils.createVideoFrame(cm.messageVideo.getPath(), 0l);
//                }
//                catch (IOException e)
//                {
//                    throw new RuntimeException(e);
//                }
//            }
//            else if(replyMessageAvailable())
//            {
//                Toast.makeText(NewCameraActivity.this, R.string.couldnt_find_message, Toast.LENGTH_LONG).show();
//            }
//            return cm;
        }

        @Override
        protected void onPostExecute(ChatMessage chatMessage)
        {
            messageLoaded(chatMessage);
        }
    }

    private void prepareEmail(final File videoFile, final ChatMessage originalMessage, final VideoUtils.VideoMetadata originalVideoMetadata)
    {
        if (AppPrefs.getInstance(NewCameraActivity.this).getPrefSelectedEmail() != null)
        {
            sendEmail(videoFile, originalMessage, originalVideoMetadata);
        }
        else
        {
            Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
            String googleAccountTypeString = "com.google";
            Account[] accounts = AccountManager.get(NewCameraActivity.this).getAccounts();
            final ArrayList<String> emailList = new ArrayList<String>();
            for (Account account : accounts)
            {
                if (emailPattern.matcher(account.name).matches() && account.type.startsWith(googleAccountTypeString))
                {
                    emailList.add(account.name);
                }
            }

            if (emailList.size() > 1)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(NewCameraActivity.this);
                builder.setTitle("Select your account");
                builder.setSingleChoiceItems(emailList.toArray(new String[emailList.size()]), 0, null);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        AppPrefs.getInstance(NewCameraActivity.this).setPrefSelectedEmail(emailList.get(((AlertDialog) dialog).getListView().getCheckedItemPosition()));
                        sendEmail(videoFile, originalMessage, originalVideoMetadata);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
            else
            {
                if (emailList.size() == 1)
                {
                    AppPrefs.getInstance(NewCameraActivity.this).setPrefSelectedEmail(emailList.get(0));
                }
                sendEmail(videoFile, originalMessage, originalVideoMetadata);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void sendEmail(final File videoFile, final ChatMessage originalMessage, final VideoUtils.VideoMetadata originalVideoMetadata)
    {
        new AsyncTask<Void, Void, String>()
        {
            @Override
            protected String doInBackground(Void... params)
            {
                File outZip = buildZipToSend(videoFile, originalMessage, originalVideoMetadata);

                String recipientId = playbackMessage != null ? playbackMessage.getSenderId() : null;

                try
                {
                    return (String) new PostSubmitMessageRequest(NewCameraActivity.this, outZip.getAbsolutePath(), recipientId).execute();
                }
                catch (TransientException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    return null;
                }
                catch (PermanentException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String messageId)
            {
                Log.d("######### SENDING MESSAGE ID: ", messageId);
                String sendTo = "";
                if (originalMessage != null && originalMessage.metadata.senderId != null)
                    sendTo = originalMessage.metadata.senderId.trim();

                //Not sure how this would creep in there, but deal with it.
                if (sendTo.equalsIgnoreCase("null"))
                    sendTo = "";

                String uriText = "mailto:" + sendTo;

                Uri mailtoUri = Uri.parse(uriText);

                boolean gmailOk = false;

                if (AppPrefs.getInstance(NewCameraActivity.this).getPrefSelectedEmail() != null)
                {
                    Intent gmailIntent = new Intent();
                    gmailIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                    gmailIntent.setData(mailtoUri);
                    gmailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.message_subject));
                    String messageLink = "<a href=\"http://www.chatwala.com/?" + messageId + "\">View the message</a>.";
                    gmailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Chatwala is a new way to have real conversations with friends. <a href=\"http://www.chatwala.com\">Get the App</a>. " + messageLink));

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
                }

                if (!gmailOk)
                {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);

                    intent.setData(mailtoUri);
                    intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.message_subject));
                    String messageLink = "<a href=\"http://www.chatwala.com/?" + messageId + "\">View the message</a>.";
                    intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Chatwala is a new way to have real conversations with friends. <a href=\"http://www.chatwala.com\">Get the App</a>. " + messageLink));

                    startActivity(Intent.createChooser(intent, "Send email..."));
                }

            }
        }.execute();
    }

    private File buildZipToSend(final File videoFile, final ChatMessage originalMessage, final VideoUtils.VideoMetadata originalVideoMetadata)
    {
        File rootDataFolder = CameraUtils.getRootDataFolder(NewCameraActivity.this);
        File buildDir = new File(rootDataFolder, "chat_" + System.currentTimeMillis());
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

            MessageMetadata openedMessageMetadata = originalMessage == null ? null : originalMessage.metadata;

            MessageMetadata sendMessageMetadata = openedMessageMetadata == null ? new MessageMetadata() : openedMessageMetadata.copy();

            sendMessageMetadata.incrementForNewMessage();

            long startRecordingMillis = openedMessageMetadata == null ? 0 : Math.round(openedMessageMetadata.startRecording * 1000d);
            long chatMessageDuration = originalVideoMetadata == null ? 0 : (originalVideoMetadata.duration + VIDEO_PLAYBACK_START_DELAY);
            sendMessageMetadata.startRecording = ((double) Math.max(chatMessageDuration - startRecordingMillis, 0)) / 1000d;

            String myEmail = AppPrefs.getInstance(NewCameraActivity.this).getPrefSelectedEmail();

            if (myEmail == null && originalMessage != null)
                myEmail = originalMessage.probableEmailSource;

            sendMessageMetadata.senderId = SharedPrefsUtils.getUserId(NewCameraActivity.this);

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

    public static void startMeWithId(Context context, String messageId)
    {
        Intent intent = new Intent(context, NewCameraActivity.class);
        intent.putExtra(MESSAGE_ID, messageId);
        context.startActivity(intent);
    }
}
