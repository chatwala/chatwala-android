package com.chatwala.android.activity;

import android.app.AlertDialog;
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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.*;
import com.chatwala.android.activity.SettingsActivity.DeliveryMethod;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.http.GetMessageFileRequest;
import com.chatwala.android.http.PostSubmitMessageRequest;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.superbus.PostSubmitMessageCommand;
import com.chatwala.android.superbus.PutMessageFileCommand;
import com.chatwala.android.superbus.PutMessageFileWithSasCommand;
import com.chatwala.android.ui.CameraPreviewView;
import com.chatwala.android.ui.CroppingLayout;
import com.chatwala.android.ui.DynamicTextureVideoView;
import com.chatwala.android.ui.TimerDial;
import com.chatwala.android.util.*;
import com.espian.showcaseview.ShowcaseView;
import com.espian.showcaseview.targets.ViewTarget;
import com.j256.ormlite.dao.Dao;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.chatwala.android.http.BaseHttpRequest;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 11/18/13
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewCameraActivity extends BaseNavigationDrawerActivity {
    public static final String INITIATOR_EXTRA = "initiator";

    public static final int RECORDING_TIME = 10000;
    public static final int VIDEO_PLAYBACK_START_DELAY = 500;
    public static final String HANGOUTS_PACKAGE_NAME = "com.google.android.talk";
    private boolean wasFirstButtonPressed;
    private boolean shouldShowPreview;
    private int openingVolume;
    private Handler buttonDelayHandler;
    private View timerButtonContainer;

    DeliveryMethod deliveryMethod;

    private boolean isFacebookFlow = false;

    private ShowcaseView tutorialView;
    private static final int FIRST_BUTTON_TUTORIAL_ID = 1000;

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

    private static enum MessageOrigin {
        INITIATOR,
        LINK,
        INBOX
    }

    private MessageOrigin getCurrentMessageOrigin() {
        if(incomingMessage != null) {
            if("unknown_recipient".equals(incomingMessage.getRecipientId())) {
                return MessageOrigin.LINK;
            }
            else {
                return MessageOrigin.INBOX;
            }
        }
        else {
            return MessageOrigin.INITIATOR;
        }
    }

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
        Logger.i("Set app state to " + appState + " at " + System.currentTimeMillis());

        //TODO get rid of this
        AndroidUtils.isMainThread();

        analyticsStateEnd(appState, buttonPress);

        this.appState = appState;
        toggleDrawerEnabled(wasFirstButtonPressed && this.appState.shouldEnableDrawer());

        delayButtonPress();

        switch (this.appState)
        {
            case ReadyStopped:
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                timerKnob.setVisibility(View.VISIBLE);
                if (incomingMessage != null)
                    timerKnob.setImageResource(R.drawable.ic_action_playback_play);
                else
                    timerKnob.setImageResource(R.drawable.record_circle);
                break;
            case PlaybackOnly:
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                analyticsTimerReset();
                setTimerKnobForRecording();
                break;
            case PlaybackRecording:
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                analyticsTimerReset();
                setTimerKnobForRecording();
                break;
            case Recording:
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                analyticsTimerReset();
                setTimerKnobForRecording();
                break;
            case PreviewReady:
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if(shouldShowPreview || isFacebookFlow || (getCurrentMessageOrigin() == MessageOrigin.INITIATOR && deliveryMethod == DeliveryMethod.FB)) {
                    startPreview();
                }
                else {
                    triggerButtonAction(true);
                }
                break;
            case Sharing:
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                closeRecordPreviewView.setVisibility(View.GONE);
                break;
            default:
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                timerKnob.setVisibility(View.INVISIBLE);
        }
    }

    private void startPreview() {
        timerKnob.setVisibility(View.VISIBLE);
        timerKnob.setImageResource(R.drawable.ic_action_send_ios);
        int messageRes = R.string.send_instructions;
        if(isFacebookFlow || (getCurrentMessageOrigin() == MessageOrigin.INITIATOR && deliveryMethod == DeliveryMethod.FB)) {
            messageRes = R.string.facebook_flow_send_instructions;
        }
        showMessage(bottomFrameMessage, bottomFrameMessageText, R.color.message_background_clear, messageRes);
        CWAnalytics.sendPreviewStartEvent();
    }

    private void analyticsTimerReset()
    {
        analyticsTimerStart = System.currentTimeMillis();
    }

    private void analyticsStateEnd(AppState newAppState, boolean buttonPress)
    {
        long duration = analyticsDuration();
        MessageOrigin origin = getCurrentMessageOrigin();
        switch (this.appState)
        {
            case PlaybackOnly:
                if(origin == MessageOrigin.INBOX) {
                    if(newAppState == AppState.Transition) {
                        CWAnalytics.sendReviewStopEvent(duration);
                    }
                    else if(newAppState == AppState.Off) {
                        CWAnalytics.sendBackgroundWhileReviewEvent(duration);
                    }
                    else {
                        CWAnalytics.sendReviewCompleteEvent(duration);
                        CWAnalytics.sendReactionStartEvent();
                    }
                }
                break;
            case PlaybackRecording:
                if(newAppState == AppState.Transition) {
                    CWAnalytics.sendReactionStopEvent(duration);
                }
                else if(newAppState == AppState.Off) {
                    CWAnalytics.sendBackgroundWhileReactionEvent(duration);
                }
                else {
                    CWAnalytics.sendReactionCompleteEvent(duration);
                    CWAnalytics.sendReplyStartEvent();
                }
                break;
            case Recording:
                if(origin == MessageOrigin.INITIATOR) {
                    if(newAppState == AppState.ReadyStopped) {
                        CWAnalytics.sendRecordingStopEvent(duration);
                    }
                    else if(newAppState == AppState.Off) {
                        CWAnalytics.sendBackgroundWhileRecordingEvent(duration);
                    }
                    else {
                        CWAnalytics.sendRecordingCompleteEvent(duration);
                    }
                }
                else {
                    if(newAppState == AppState.ReadyStopped) {
                        CWAnalytics.sendReplyStopEvent(duration);
                    }
                    else if(newAppState == AppState.Off) {
                        CWAnalytics.sendBackgroundWhileReplyEvent(duration);
                    }
                    else {
                        CWAnalytics.sendReplyCompleteEvent(duration);
                    }
                }

                break;
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
        Logger.i("Beginning of onCreate()");

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
                triggerButtonAction(true);
            }
        });
        videoViewContainer.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                triggerButtonAction(false);
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
                Logger.logUserAction("Close record preview pressed in state: " + getAppState().name());
                CWAnalytics.sendRedoMessageEvent((long) recordPreviewCompletionListener.replays);
                closeResultPreview();
            }
        });

        if(savedInstanceState != null && savedInstanceState.containsKey(PENDING_SEND_URL))
        {
            recordPreviewFile = new File(savedInstanceState.getString(PENDING_SEND_URL));
        }

        if(getIntent().hasExtra(OPEN_DRAWER))
        {
            openDrawer();
        }

        if(getIntent().getData() != null) {
            if("fb".equals(getIntent().getData().getLastPathSegment())) {
                CWAnalytics.sendFacebookInitiatorEvent();
                getIntent().setData(null);
                isFacebookFlow = true;
            }
        }

//        captureOpeningVolume();

        Logger.i("End of onCreate()");
    }

    private void showTutorialIfNeeded() {
        if(tutorialView != null) {
            tutorialView.hide();
        }

        if(replyMessageAvailable()) {
            ShowcaseView.registerShot(this, FIRST_BUTTON_TUTORIAL_ID);
            return;
        }

        ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
        co.showcaseId = FIRST_BUTTON_TUTORIAL_ID;
        co.hideOnClickOutside = false;
        co.shotType = ShowcaseView.TYPE_ONE_SHOT;

        ViewTarget target = new ViewTarget(R.id.timerDial, this);
        tutorialView = ShowcaseView.insertShowcaseView(target, this, "Welcome to Chatwala.", "A new way to have conversations. Tap to get started.", co);
    }

    private void runWaterSplash()
    {
        Logger.i("Start of runWaterSplash()");
        if (splash != null)
        {
            final ViewGroup root = findViewRoot();
            root.removeView(splash);
        }

        splash = getLayoutInflater().inflate(R.layout.splash_ripple, null);
        final ViewGroup root = findViewRoot();
        root.addView(splash);
        Logger.i("End of runWaterSplash()");
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

        wasFirstButtonPressed = AppPrefs.getInstance(this).wasFirstButtonPressed();
        shouldShowPreview = AppPrefs.getInstance(this).getPrefShowPreview();
        deliveryMethod = AppPrefs.getInstance(NewCameraActivity.this).getDeliveryMethod();

        Logger.i();
        CWAnalytics.setStarterMessage(!replyMessageAvailable());

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
        CWAnalytics.resetRedos();
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
        super.onPause();

        if (heartbeatTimer != null)
            heartbeatTimer.abort();

        activityActive = false;

        isFacebookFlow = false;

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

    private void triggerButtonAction(boolean fromCenterButtonPress)
    {
        AppState state = getAppState();
        Logger.logUserAction("Timer button pressed in state: " + state.name());

        //only handle two clickable state from bottom screen
        if(!fromCenterButtonPress && (state != AppState.ReadyStopped && state!=AppState.PreviewReady)) {
            return;
        }

        if(!wasFirstButtonPressed) {
            wasFirstButtonPressed = true;
            AppPrefs.getInstance(this).setFirstButtonPressed();
        }

        if(tutorialView != null) {
            ShowcaseView.registerShot(this, tutorialView.getConfigOptions().showcaseId);
            tutorialView.hide();
        }

        //Don't do anything.  These should be very short states.
        if (state == AppState.Off || state == AppState.Transition || state == AppState.LoadingFileCamera || state == AppState.RecordingLimbo || state == AppState.PreviewLoading || state == AppState.Sharing)
            return;

        if (state == AppState.ReadyStopped)
        {
            MessageOrigin origin = getCurrentMessageOrigin();
            if(origin == MessageOrigin.INITIATOR) {
                CWAnalytics.sendRecordingStartEvent(fromCenterButtonPress);
            }
            else if(origin == MessageOrigin.LINK) {
                CWAnalytics.sendReactionStartEvent(fromCenterButtonPress);
            }
            else if(origin == MessageOrigin.INBOX) {
                CWAnalytics.sendReviewStartEvent(fromCenterButtonPress);
            }
            startPlaybackRecording();
            return;
        }

        if (state == AppState.PlaybackOnly)
        {
            abortBeforeRecording();
            return;
        }

        if (state == AppState.PlaybackRecording)
        {
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
                            Logger.e("Error loading playback for share", e);
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
                            final String messageSasUrl = messageToSendDirectly.getUrl();
                            messageToSendDirectly = null;

                            final File outFile = ZipUtil.buildZipToSend(NewCameraActivity.this, recordPreviewFile, incomingMessage, chatMessageVideoMetadata, messageId);

                            DataProcessor.runProcess(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    BusHelper.submitCommandSync(NewCameraActivity.this, new PutMessageFileWithSasCommand(outFile.getAbsolutePath(), messageId, messageSasUrl, null, "unknown_recipient"));
                                }
                            });

                            CWAnalytics.sendSendMessageEvent(deliveryMethod, (long) recordPreviewCompletionListener.replays);
                            if(isFacebookFlow) {
                                sendFacebookPostShare(messageId);
                            }
                            else if (deliveryMethod == DeliveryMethod.SMS) {
                                sendSms(messageId);
                            }
                            else if(deliveryMethod == DeliveryMethod.CWSMS) {
                                sendChatwalaSms(messageId);
                            }
                            else if(deliveryMethod == DeliveryMethod.EMAIL) {
                                sendEmail(messageId);
                            }
                            else {
                                sendFacebookPostShare(messageId);
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
        Logger.i();
        cameraPreviewView.abortRecording();
        abortBeforeRecording();
    }

    @Override
    public void onBackPressed()
    {
        CWAnalytics.sendBackPressedEvent();
        Logger.logUserAction("onBackPressed");
        AppState state = getAppState();
        if (state == AppState.PreviewReady)
            closeResultPreview();
        else
            super.onBackPressed();
    }

    private void abortBeforeRecording()
    {
        Logger.i();
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
            Logger.i("HeartbeatTimer.abort");
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
            Logger.i("HeartbeatTimer.endPause");
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
                    Logger.i("HeartbeatTimer.run#startRecordingBlock");
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
                    Logger.i("Stop HeartbeatTimer");
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
        Logger.i();

        //TODO get rid of this
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
        Logger.i();

        //TODO get rid of this
        AndroidUtils.isMainThread();

        if(getAppState() == AppState.ReadyStopped) {
            showRecordingCountdown(false);
        }

        setAppState(AppState.RecordingLimbo);
        if (messageVideoView != null)
            messageVideoView.pause();
        cameraPreviewView.startRecording();
    }

    private void stopRecording(boolean buttonPress)
    {
        Logger.i();

        //TODO get rid of this
        AndroidUtils.isMainThread();

        hideMessage(bottomFrameMessage);
        if (heartbeatTimer != null)
            heartbeatTimer.abort();

        if(buttonPress) {
            CWAnalytics.sendStopPressedEvent(analyticsDuration());
        }

        if(buttonPress && getCurrentMessageOrigin() != MessageOrigin.INITIATOR && !shouldShowPreview) {
            cameraPreviewView.stopRecording();
            showSendOrCancelAlert();
        }
        else {
            setAppState(AppState.PreviewLoading, buttonPress);
            cameraPreviewView.stopRecording();
        }
    }

    private void showSendOrCancelAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.send_or_cancel_title)
                .setCancelable(false)
                .setPositiveButton(R.string.send_or_cancel_send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setAppState(AppState.PreviewLoading, true);
                    }
                })
                .setNegativeButton(R.string.send_or_cancel_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setAppState(AppState.ReadyStopped, true);
                        tearDownSurface();
                        createSurface();
                    }
                }).create().show();
    }


    private void previewSurfaceReady()
    {
        Logger.i();
        showTutorialIfNeeded();
        initStartState();
    }

    private void initStartState()
    {
        Logger.i();
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
        Logger.i();
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
                    //showMessage(bottomFrameMessage, bottomFrameMessageText, R.color.message_background_alpha, R.string.now_record_reply);
                    showRecordingCountdown(true);
                }
            });
            showMessage(topFrameMessage, topFrameMessageText, R.color.message_background_alpha, R.string.play_message_record_reaction);
        }
        else
        {
            removeWaterSplash();
            int messageRes = R.string.basic_instructions;
            if(isFacebookFlow || deliveryMethod == DeliveryMethod.FB) {
                messageRes = R.string.facebook_flow_instructions;
            }
            showMessage(bottomFrameMessage, bottomFrameMessageText, R.color.message_background_clear, messageRes);
        }

        liveForRecording();
    }

    private void liveForRecording()
    {
        Logger.i();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppState appStateTest = getAppState();
                Logger.i("AppState = " + appStateTest);
                if (appStateTest == AppState.LoadingFileCamera) {
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
                while(getAppState() != AppState.PreviewLoading) {
                    try {
                        Thread.sleep(500);
                        if(getAppState() == AppState.ReadyStopped || getAppState() == AppState.LoadingFileCamera) {
                            params[0].delete();
                            cancel(true);
                            return null;
                        }
                    }
                    catch(Exception e) {
                        return null;
                    }
                }
                return VideoUtils.findMetadata(params[0]);
            }
            catch (IOException e)
            {
                Logger.e("Got an IOException while getting the video metadata", e);
                return null;
            }

        }

        @Override
        protected void onPostExecute(VideoUtils.VideoMetadata videoInfo)
        {
            recordPreviewVideoView = new DynamicTextureVideoView(NewCameraActivity.this, recordPreviewFile, videoInfo.width, videoInfo.height, videoInfo.rotation, null, false);
            recordPreviewCompletionListener = new ReplayCountingCompletionListener();

            if(shouldShowPreview || isFacebookFlow || (getCurrentMessageOrigin() == MessageOrigin.INITIATOR && deliveryMethod == DeliveryMethod.FB)) {
                cameraPreviewContainer.addView(recordPreviewVideoView);
                if(!isFacebookFlow) {
                    closeRecordPreviewView.setVisibility(View.VISIBLE);
                }
                recordPreviewVideoView.start();
                recordPreviewVideoView.setOnCompletionListener(recordPreviewCompletionListener);
            }

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
        Logger.i();

        //TODO get rid of this
        AndroidUtils.isMainThread();

        if (replyMessageAvailable())
        {
            runWaterSplash();
        }
        else
        {
            prepManualSend();
        }

        setAppState(AppState.LoadingFileCamera);
        hideMessage(topFrameMessage);
        hideMessage(bottomFrameMessage);
        Logger.i("Starting createSurface work");
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
        Logger.i("cameraPreviewView added to our layout");
    }

    private void showResultPreview(File videoFile)
    {
        Logger.i();

        //TODO get rid of this
        AndroidUtils.isMainThread();

        this.recordPreviewFile = videoFile;
        tearDownSurface();
        new LoadAndShowVideoMessageTask().execute(recordPreviewFile);
    }

    private void closeResultPreview()
    {
        Logger.i();

        //TODO get rid of this
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
        Logger.i("Start of tearDownSurface");
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
        Logger.i("End of tearDownSurface");
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
                    Logger.i("Refreshing message " + playbackMessageId);
                    playbackMessage = new ChatwalaMessage();
                    playbackMessage.setMessageId(playbackMessageId);
                    playbackMessage = (ChatwalaMessage)new GetMessageFileRequest(NewCameraActivity.this, playbackMessage).execute();

                    if(playbackMessage == null)
                        throw new IOException();
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
                        Logger.e("Couldn't get message ID", e);
                    }
                    catch (PermanentException e)
                    {
                        Logger.e("Couldn't get message ID", e);
                    }
                    attempts++;
                }
            }
        });
    }

    private void sendFacebookPostShare(String messageId) {
        //http://stackoverflow.com/questions/7545254/android-and-facebook-share-intent
        String urlToShare = EnvironmentVariables.get().getWebPath() + messageId;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, urlToShare);

        // See if official Facebook app is found
        boolean facebookAppFound = false;
        List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo info : matches) {
            if (info.activityInfo.packageName.toLowerCase().startsWith("com.facebook")) {
                intent.setPackage(info.activityInfo.packageName);
                facebookAppFound = true;
                break;
            }
        }

        // As fallback, launch sharer.php in a browser
        if (!facebookAppFound) {
            String sharerUrl = "https://www.facebook.com/sharer/sharer.php?u=" + urlToShare;
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sharerUrl));
        }

        closePreviewOnReturn = true;

        startActivity(intent);
    }

    @SuppressWarnings("unchecked")
    private void sendEmail(final String messageId)
    {
        Logger.i("Sending message ID " + messageId);
        String uriText = "mailto:";

        Uri mailtoUri = Uri.parse(uriText);
        //String messageLink = "<a href=\"http://chatwala.com/?" + messageId + "\">View the message</a>.";
        String messageLink = EnvironmentVariables.get().getWebPath() + messageId;

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
            Logger.e("Couldn't send GMail", ex);
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
        String messageLink = EnvironmentVariables.get().getWebPath() + messageId;
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

    private void sendChatwalaSms(final String messageId)
    {
        String messageUrl = EnvironmentVariables.get().getWebPath() + messageId;
        String messageText = "Hey, I sent you a video message on Chatwala";
        closePreviewOnReturn = true;
        Intent i = new Intent(this, SmsActivity.class);
        i.putExtra(SmsActivity.SMS_MESSAGE_URL_EXTRA, messageUrl);
        i.putExtra(SmsActivity.SMS_MESSAGE_EXTRA, messageText);
        startActivity(i);
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

    private abstract class RecordCountdownRunnable implements Runnable {
        private int countdownBegin;
        private int countdownEnd;
        private boolean isReply;
        private boolean showFirstMessage = true;
        private String displayMessage;

        public RecordCountdownRunnable(int countdownBegin, int countdownEnd, boolean isReply) {
            this.countdownBegin = countdownBegin;
            this.countdownEnd = countdownEnd;
            this.isReply = isReply;

            displayMessage = getString((isReply ? R.string.recording_reply_countdown : R.string.recording_countdown));
        }

        public boolean shouldShowFirst() {
            return showFirstMessage;
        }

        public void setShowFirstMessage(boolean showFirstMessage) {
            this.showFirstMessage = showFirstMessage;
        }

        public int tick() {
            return --countdownBegin;
        }

        public boolean isCountdownValid() {
            return countdownBegin > countdownEnd;
        }

        public String getDisplayMessage() {
            if(!shouldShowPreview && isReply && countdownBegin == 5) {
                displayMessage = getString(R.string.sending_reply_countdown);
            }
            return displayMessage;
        }
    }

    private void showRecordingCountdown(final boolean isReply) {
        int colorRes = (isReply ? R.color.message_background_alpha : R.color.message_background_clear);
        bottomFrameMessage.setBackgroundColor(getResources().getColor(colorRes));
        bottomFrameMessageText.setText("");
        bottomFrameMessage.setVisibility(View.VISIBLE);
        bottomFrameMessageText.setVisibility(View.VISIBLE);

        final int delay = (isReply ? 850 : 1000);

        bottomFrameMessageText.postDelayed(new RecordCountdownRunnable(10, 0, isReply) {
            @Override
            public void run() {
                if (bottomFrameMessageText == null) { //if we lost the activity
                    bottomFrameMessageText.removeCallbacks(this);
                    return;
                }

                if (!isCountdownValid() ||
                        (getAppState() != AppState.Recording && getAppState() != AppState.PlaybackRecording && getAppState() != AppState.RecordingLimbo)) {
                    bottomFrameMessageText.removeCallbacks(this);
                }
                else {
                    if (shouldShowFirst()) {
                        String message = getString((isReply ? R.string.recording_reply_countdown : R.string.recording_countdown));
                        bottomFrameMessageText.setText(message.replace("%d", " "));
                        setShowFirstMessage(false);
                    }
                    else {
                        bottomFrameMessageText.setText(String.format(getDisplayMessage(), tick()));
                    }
                    bottomFrameMessageText.postDelayed(this, delay);
                }
            }
        }, 100);
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

    public static void startMe(final Context context)
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                context.startActivity(new Intent(context, NewCameraActivity.class));
            }
        }, 100);
    }

    public static void startMeWithId(final Context context, final String messageId)
    {
        Intent intent = new Intent(context, NewCameraActivity.class);
        intent.putExtra(MESSAGE_ID, messageId);
        context.startActivity(intent);
    }
}
