package com.chatwala.android.activity;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v4.content.LocalBroadcastManager;
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
import com.chatwala.android.AppPrefs;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.R;
import com.chatwala.android.activity.SettingsActivity.DeliveryMethod;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.http.GetMessageFileRequest;
import com.chatwala.android.http.server20.ChatwalaMessageStartInfo;
import com.chatwala.android.http.server20.ChatwalaResponse;
import com.chatwala.android.http.server20.GetShareUrlFromMessageIdRequest;
import com.chatwala.android.http.server20.PostAddToInboxRequest;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.receivers.ReferrerReceiver;
import com.chatwala.android.sms.Sms;
import com.chatwala.android.sms.SmsManager;
import com.chatwala.android.superbus.PostSubmitMessageCommand;
import com.chatwala.android.superbus.server20.NewMessageFlowCommand;
import com.chatwala.android.superbus.server20.ReplyFlowCommand;
import com.chatwala.android.ui.CameraPreviewView;
import com.chatwala.android.ui.CroppingLayout;
import com.chatwala.android.ui.DynamicTextureVideoView;
import com.chatwala.android.ui.TimerDial;
import com.chatwala.android.util.AndroidUtils;
import com.chatwala.android.util.CWAnalytics;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.Referrer;
import com.chatwala.android.util.ShareUtils;
import com.chatwala.android.util.VideoUtils;
import com.espian.showcaseview.ShowcaseView;
import com.espian.showcaseview.targets.ViewTarget;
import com.j256.ormlite.dao.Dao;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 11/18/13
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewCameraActivity extends DrawerListActivity {
    public static final String INITIATOR_EXTRA = "initiator";
    private static final int FACEBOOK_DELIVERY_REQUEST_CODE = 1000;

    public static final int RECORDING_TIME = 10000;
    public static final int VIDEO_PLAYBACK_START_DELAY = 500;
    public static final String HANGOUTS_PACKAGE_NAME = "com.google.android.talk";
    private boolean wasFirstButtonPressed;
    private boolean shouldShowPreview;
    private int openingVolume;
    private Handler buttonDelayHandler;
    private View timerButtonContainer;

    private DeliveryMethod deliveryMethod;
    private Referrer referrer;
    private Intent referrerIntent;

    private String recordCopyOverride;

    private boolean isFacebookFlow = false;
    private ArrayList<String> topContactsList;

    private ShowcaseView tutorialView;
    private static final int FIRST_BUTTON_TUTORIAL_ID = 1000;

    private ChatwalaMessage playbackMessage = null;
    private ChatwalaMessageStartInfo messageStartInfo = null;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<ChatwalaMessageStartInfo> messageStartInfoFuture;
    private static final String MESSAGE_READ_URL_EXTRA = "MESSAGE_READ_URL";
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
            if("RECIPIENT_UNKNOWN".equals(incomingMessage.getRecipientId())) {
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
        showMessage(bottomFrameMessage, bottomFrameMessageText, R.color.message_background_clear, getPreviewCopy());
        CWAnalytics.sendPreviewStartEvent();
    }

    private String getPreviewCopy() {
        if(isFacebookFlow || (getCurrentMessageOrigin() == MessageOrigin.INITIATOR && deliveryMethod == DeliveryMethod.FB)) {
            return getString(R.string.facebook_flow_send_instructions);
        }
        else {
            return getString(R.string.send_instructions);
        }
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

    private BroadcastReceiver referrerIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(getAppState() == AppState.ReadyStopped) {
                referrerIntent = intent;
                Logger.i("Received referrer intent from ReferrerReceiver");
                findReferrer();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.i("Beginning of onCreate()");

        if(!getIntent().hasExtra(TopContactsActivity.TOP_CONTACTS_LIST_EXTRA)) {
            if(!AppPrefs.getInstance(this).wasTopContactsShown() ||
                    (AppPrefs.getInstance(this).getDeliveryMethod() == DeliveryMethod.TOP_CONTACTS &&
                    getIntent().getData() == null && !getIntent().hasExtra(OPEN_DRAWER) &&
                    !getIntent().hasExtra(MESSAGE_ID))) {
                startActivity(new Intent(this, TopContactsActivity.class));
                AppPrefs.getInstance(this).setTopContactsShown(true);
                finish();
                return;
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(referrerIntentReceiver,
                new IntentFilter(ReferrerReceiver.CW_REFERRER_ACTION));

        findReferrer();

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

//        captureOpeningVolume();

        Logger.i("End of onCreate()");
    }

    private void findReferrer() {
        String referrerPref = AppPrefs.getInstance(this).getReferrer();
        if(referrerIntent != null && referrerIntent.hasExtra(ReferrerReceiver.REFERRER_EXTRA)) {
            referrer = referrerIntent.getParcelableExtra(ReferrerReceiver.REFERRER_EXTRA);
            referrerIntent = null;
        }
        else if(referrerPref != null) {
            referrer = new Referrer(referrerPref);
        }
        else if(getIntent().getData() != null) {
            referrer = new Referrer(getIntent().getData());
            if(referrer.isValid()) {
                getIntent().setData(null);
            }
        }

        if(referrer != null && referrer.isValid()) {
            CWAnalytics.sendReferrerReceivedEvent(referrer);
            if(referrer.isFacebookReferrer()) {
                isFacebookFlow = true;
            }
            /*else if(referrer.isMessageReferrer()) {
                getIntent().putExtra(MESSAGE_ID, referrer.getValue());
            }
            else if(referrer.isCopyReferrer()) {
                recordCopyOverride = referrer.getValue();
            }*/
        }
    }

    private void showTutorialIfNeeded() {
        if(tutorialView != null) {
            tutorialView.hide();
        }

        if(replyMessageAvailable() || deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
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
    public void onResume() {
        super.onResume();

        wasFirstButtonPressed = AppPrefs.getInstance(this).wasFirstButtonPressed();
        shouldShowPreview = AppPrefs.getInstance(this).getPrefShowPreview();
        deliveryMethod = AppPrefs.getInstance(NewCameraActivity.this).getDeliveryMethod();
        if(getIntent().hasExtra(TopContactsActivity.TOP_CONTACTS_LIST_EXTRA) &&
                getIntent().getStringArrayListExtra(TopContactsActivity.TOP_CONTACTS_LIST_EXTRA).size() > 0) {
            topContactsList = getIntent().getStringArrayListExtra(TopContactsActivity.TOP_CONTACTS_LIST_EXTRA);
            getIntent().removeExtra(TopContactsActivity.TOP_CONTACTS_LIST_EXTRA);
            deliveryMethod = DeliveryMethod.TOP_CONTACTS;
        }
        else if(deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
            deliveryMethod = AppPrefs.getInstance(this).getDeliveryMethod();
            if(deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
                deliveryMethod = DeliveryMethod.CWSMS;
            }
            topContactsList = null;
        }

        Logger.i();
        CWAnalytics.setStarterMessage(!replyMessageAvailable(), referrer);

        activityActive = true;
        setAppState(AppState.Transition);

//        setReviewVolume();

        if(deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
            createSurface();
        }
        else if (closePreviewOnReturn)
        {
            closePreviewOnReturn = false;
            closeResultPreview();
        }
        else if (hasPendingSendMessage())
        {
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

        if(referrer != null && referrer.isFacebookReferrer() && getAppState() == AppState.PreviewReady) {
            closeResultPreview();
        }
        referrer = null;
        isFacebookFlow = false;

        AppState state = getAppState();

        if (state == AppState.PlaybackRecording || state == AppState.Recording)
        {
            if(cameraPreviewView != null) {
                cameraPreviewView.abortRecording();
            }
        }

        setAppState(AppState.Off);

//        resetOpeningVolume();
        tearDownSurface();

    }

    @Override
    public void onStop() {
        super.onStop();

        //shouldn't need to handle anything, but unregistering receivers sometimes crashes unexpectedly
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(referrerIntentReceiver);
        } catch(Exception ignore) {}
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

    private void triggerButtonAction(boolean fromCenterButtonPress) {
        triggerButtonAction(fromCenterButtonPress, false);
    }

    private void triggerButtonAction(boolean fromCenterButtonPress, boolean autoStarted)
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
                CWAnalytics.sendRecordingStartEvent(fromCenterButtonPress, autoStarted);
                messageStartInfoFuture = executor.submit(new Callable<ChatwalaMessageStartInfo>() {

                    @Override
                    public ChatwalaMessageStartInfo call() throws Exception {
                        return prepManualSend();
                    }
                });
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

                    if (playbackMessage != null)
                    {

                        DataProcessor.runProcess(new Runnable() {
                            @Override
                            public void run() {
                                if(recordPreviewFile != null) {
                                    BusHelper.submitCommandSync(NewCameraActivity.this, new ReplyFlowCommand(incomingMessage, UUID.randomUUID().toString(), recordPreviewFile.getPath(), chatMessageVideoMetadata.duration));
                                }
                            }
                        });
                        return true;
                    }
                    else
                    {

                        if (playbackMessage == null || playbackMessage.getSenderId().startsWith("unknown"))
                        {
                            try {
                                messageStartInfo = messageStartInfoFuture.get();
                            }
                            catch(Exception e) {
                                Logger.e("Got an exception while waiting for the messageStartInfo", e);
                                messageStartInfo = null;
                            }
                            if(messageStartInfo == null) {
                                return false;
                            }
                            final String messageId = messageStartInfo.getMessageId();

                            DataProcessor.runProcess(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if(recordPreviewFile != null) {
                                        BusHelper.submitCommandSync(NewCameraActivity.this, new NewMessageFlowCommand(messageId, recordPreviewFile.getPath()));
                                    }
                                }
                            });

                            if(isFacebookFlow) {
                                CWAnalytics.sendSendMessageEvent(DeliveryMethod.FB, (long) recordPreviewCompletionListener.replays);
                            }
                            else {
                                CWAnalytics.sendSendMessageEvent(deliveryMethod, (long) recordPreviewCompletionListener.replays);
                            }

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
                            else if(deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
                                sendSmsToTopContacts(messageId);
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
                protected void onPostExecute(Boolean complete) {
                    if(complete != null) {
                        if(complete) {
                            Toast.makeText(NewCameraActivity.this, "Message sent.", Toast.LENGTH_LONG).show();

                            AppPrefs prefs = AppPrefs.getInstance(NewCameraActivity.this);
                            boolean showFeedback = false;
                            if(!prefs.getPrefFeedbackShown()) {
                                showFeedback = prefs.recordMessageSent();
                            }

                            if(!prefs.isImageReviewed() && MessageDataStore.findUserImageInLocalStore(prefs.getUserId()).exists()) {
                                UpdateProfilePicActivity.startMe(NewCameraActivity.this, true);
                            }
                            else if(showFeedback) {
                                FeedbackActivity.startMe(NewCameraActivity.this, true);
                            }
                            else {
                                NewCameraActivity.startMe(NewCameraActivity.this);
                            }

                            finish();
                        }
                        else {
                            Toast.makeText(NewCameraActivity.this, "Couldn't contact server.  Please try again later.", Toast.LENGTH_LONG).show();
                            NewCameraActivity.startMe(NewCameraActivity.this);
                            finish();
                        }
                    }
                    else {
                        if(deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
                            deliveryMethod = DeliveryMethod.CWSMS;
                            if(!getIntent().hasExtra(TopContactsActivity.TOP_CONTACTS_SHOW_UPSELL_EXTRA)) {
                                tearDownSurface();
                                createSurface();
                            }
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

        recordCopyOverride = null;

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

        if(cameraPreviewView != null) {
            if(buttonPress && ((getCurrentMessageOrigin() != MessageOrigin.INITIATOR && !shouldShowPreview) ||
                    deliveryMethod == DeliveryMethod.TOP_CONTACTS)) {
                cameraPreviewView.stopRecording();
                showSendOrCancelAlert();
            }
            else {
                setAppState(AppState.PreviewLoading, buttonPress);
                cameraPreviewView.stopRecording();
            }
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
                        if(deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
                            deliveryMethod = AppPrefs.getInstance(NewCameraActivity.this).getDeliveryMethod();
                            if(deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
                                deliveryMethod = DeliveryMethod.CWSMS;
                            }
                            topContactsList = null;
                        }
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
            if(deliveryMethod == DeliveryMethod.TOP_CONTACTS && topContactsList != null) {
                showPreRecordingCountdown();
            }
            else {
                showMessage(bottomFrameMessage, bottomFrameMessageText, R.color.message_background_clear, getRecordCopy());
            }
        }

        liveForRecording();
    }

    private String getRecordCopy() {
        if(recordCopyOverride != null) {
            return recordCopyOverride;
        }
        else if(isFacebookFlow || deliveryMethod == DeliveryMethod.FB) {
            return getString(R.string.facebook_flow_instructions);
        }
        else {
            return getString(R.string.basic_instructions);
        }
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
            catch (Exception e)
            {
                Logger.e("Got an Exception while getting the video metadata", e);
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


        //TODO get rid of this
        AndroidUtils.isMainThread();

        if (replyMessageAvailable())
        {
            runWaterSplash();
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

        if(cameraPreviewContainer != null) {
            cameraPreviewContainer.removeAllViews();
            if (cameraPreviewView != null) {
                cameraPreviewView.releaseResources();
                cameraPreviewView = null;
            }
        }
        if(videoViewContainer != null) {
            videoViewContainer.removeAllViews();
        }
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
                String readUrl;
                String playbackMessageId;
                if (getIntent().hasExtra(MESSAGE_READ_URL_EXTRA) && getIntent().hasExtra(MESSAGE_ID))
                {
                    readUrl = getIntent().getStringExtra(MESSAGE_READ_URL_EXTRA);
                    playbackMessageId = getIntent().getStringExtra(MESSAGE_ID);
                }
                else
                {
                    readUrl = ShareUtils.getReadUrlFromShareUrl(getIntent().getData());
                    playbackMessageId = ShareUtils.getMessageIdFromShareUrl(getIntent().getData());
                    if(readUrl == null || playbackMessageId == null) {
                        return null;
                    }
                    new PostAddToInboxRequest(NewCameraActivity.this, playbackMessageId, AppPrefs.getInstance(NewCameraActivity.this).getUserId()).execute();
                }

                Dao<ChatwalaMessage, String> messageDao = DatabaseHelper.getInstance(NewCameraActivity.this).getChatwalaMessageDao();
                playbackMessage = messageDao.queryForId(playbackMessageId);

                if(playbackMessage == null || playbackMessage.getMessageFile() == null)
                {
                    Logger.i("Refreshing message " + playbackMessageId);
                    playbackMessage = new ChatwalaMessage();
                    playbackMessage.setMessageId(playbackMessageId);
                    playbackMessage.setReadUrl(readUrl);
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
            catch (Exception e) {
                Logger.e("Got an Exception while loading a message", e);
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

    private ChatwalaMessageStartInfo prepManualSend()
    {
        int attempts = 0;
        while (attempts < 3)
        {
            try
            {

                String messageId = UUID.randomUUID().toString();
                ChatwalaResponse<String> response = (ChatwalaResponse<String>) new GetShareUrlFromMessageIdRequest(NewCameraActivity.this, messageId).execute();

                if(response.getResponseData()!=null) {

                    messageStartInfo = new ChatwalaMessageStartInfo();
                    messageStartInfo.setShareUrl(response.getResponseData());
                    messageStartInfo.setMessageId(messageId);
                    return messageStartInfo;
                }

            }
            catch (TransientException e)
            {}
            catch (PermanentException e)
            {}
            attempts++;
        }
        return null;
    }

    private void sendFacebookPostShare(String messageId) {
        String urlToShare = messageStartInfo.getShareUrl();

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
        //startActivityForResult(intent, FACEBOOK_DELIVERY_REQUEST_CODE);
    }

    @SuppressWarnings("unchecked")
    private void sendEmail(final String messageId)
    {
        Logger.i("Sending message ID " + messageId);
        String uriText = "mailto:";

        Uri mailtoUri = Uri.parse(uriText);
        //String messageLink = "<a href=\"http://chatwala.com/?" + messageId + "\">View the message</a>.";
        //String messageLink = EnvironmentVariables.get().getWebPath() + messageId;
        String messageLink = messageStartInfo.getShareUrl();

        boolean gmailOk = false;

        Intent gmailIntent = new Intent();
        gmailIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
        gmailIntent.setData(mailtoUri);
        gmailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.message_subject));
        //gmailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Chatwala is a new way to have real conversations with friends. " + messageLink));
        gmailIntent.putExtra(Intent.EXTRA_TEXT, "Hey, I sent you a video message on Chatwala:\n\n" + messageLink);

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
            intent.putExtra(Intent.EXTRA_TEXT, "Hey, I sent you a video message on Chatwala:\n\n" + messageLink);

            startActivity(Intent.createChooser(intent, "Send email..."));
        }
    }

    private void sendSmsToTopContacts(String messageId) {
        if(topContactsList == null || topContactsList.size() == 0) {
            DeliveryMethod deliveryMethod = AppPrefs.getInstance(this).getDeliveryMethod();
            if (deliveryMethod == DeliveryMethod.SMS) {
                sendSms(messageId);
            }
            else if(deliveryMethod == DeliveryMethod.CWSMS) {
                sendChatwalaSms(messageId);
            }
            else if(deliveryMethod == DeliveryMethod.EMAIL) {
                sendEmail(messageId);
            }
            return;
        }

        String messageLink = messageStartInfo.getShareUrl();
        for (String contact : topContactsList) {
            SmsManager.getInstance().sendSms(new Sms(contact, messageLink));
        }
        CWAnalytics.sendTopContactsSentEvent(topContactsList.size());
        if(getIntent().hasExtra(TopContactsActivity.TOP_CONTACTS_SHOW_UPSELL_EXTRA)) {
            closePreviewOnReturn = true;
            Intent i = new Intent(this, SmsActivity.class);
            i.putExtra(SmsActivity.SMS_MESSAGE_URL_EXTRA, messageLink);
            i.putExtra(SmsActivity.COMING_FROM_TOP_CONTACTS_EXTRA, TopContactsActivity.INITIAL_TOP_CONTACTS);
            startActivity(i);
            CWAnalytics.sendUpsellShownEvent();
        }
    }

    private void sendSms(final String messageId)
    {
        String messageLink = messageStartInfo.getShareUrl();
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
        //String messageUrl = EnvironmentVariables.get().getWebPath() + messageId;
        String messageLink = messageStartInfo.getShareUrl();
        closePreviewOnReturn = true;
        Intent i = new Intent(this, SmsActivity.class);
        i.putExtra(SmsActivity.SMS_MESSAGE_URL_EXTRA, messageLink);
        startActivity(i);
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == FACEBOOK_DELIVERY_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                CWAnalytics.sendFacebookSendConfirmed();
            }
            else if(resultCode == RESULT_CANCELED) {
                CWAnalytics.sendFacebookSendCanceled();
            }
        }
    }*/

    private void showMessage(View messageView, TextView messageViewText, int colorRes, int messageRes) {
        showMessage(messageView, messageViewText, colorRes, getString(messageRes));
    }

    private void showMessage(View messageView, TextView messageViewText, int colorRes, String message)
    {
        if (messageView.getVisibility() != View.GONE)
            return;

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.message_fade_in);

        messageView.startAnimation(animation);
        messageView.setBackgroundColor(getResources().getColor(colorRes));
        messageView.setVisibility(View.VISIBLE);
        messageViewText.setText(message);
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

            if(deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
                displayMessage = getString(R.string.top_contacts_recording_text);
            }
            else {
                displayMessage = getString((isReply ? R.string.recording_reply_countdown : R.string.recording_countdown));
            }
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
            if(deliveryMethod == DeliveryMethod.TOP_CONTACTS && countdownBegin == 5) {
                displayMessage = getString(R.string.top_contacts_sending_text);
            }
            else if(!shouldShowPreview && isReply && countdownBegin == 5) {
                displayMessage = getString(R.string.sending_reply_countdown);
            }
            return displayMessage;
        }
    }

    private void showPreRecordingCountdown() {
        int colorRes = (R.color.message_background_clear);
        bottomFrameMessage.setBackgroundColor(getResources().getColor(colorRes));
        bottomFrameMessageText.setText("");
        bottomFrameMessage.setVisibility(View.VISIBLE);
        bottomFrameMessageText.setVisibility(View.VISIBLE);

        final int delay = 1000;

        if(deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
            bottomFrameMessageText.postDelayed(new RecordCountdownRunnable(4, 1, false) {
                @Override
                public void run() {
                    if (bottomFrameMessageText == null) { //if we lost the activity
                        bottomFrameMessageText.removeCallbacks(this);
                        return;
                    }

                    if (!isCountdownValid() || getAppState() != AppState.ReadyStopped) {
                        if(!isCountdownValid()) {
                            triggerButtonAction(false, true);
                        }
                        bottomFrameMessageText.removeCallbacks(this);
                    }
                    else {
                        bottomFrameMessageText.setText(String.format(getString(R.string.top_contacts_pre_recording_text), tick()));
                        bottomFrameMessageText.postDelayed(this, delay);
                    }
                }
            }, 100);
        }
        else {
            showRecordingCountdown(false);
        }
    }

    private void showRecordingCountdown(final boolean isReply) {
        int colorRes = (isReply ? R.color.message_background_alpha : R.color.message_background_clear);
        bottomFrameMessage.setBackgroundColor(getResources().getColor(colorRes));
        bottomFrameMessageText.setText("");
        bottomFrameMessage.setVisibility(View.VISIBLE);
        bottomFrameMessageText.setVisibility(View.VISIBLE);

        final int delay = (isReply ? 850 : 1000);

        bottomFrameMessageText.postDelayed(new RecordCountdownRunnable(10, 1, isReply) {
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
                        if(deliveryMethod == DeliveryMethod.TOP_CONTACTS) {
                            message = getString(R.string.top_contacts_recording_text);
                        }
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

    public static void startMeWithId(final Context context, String messageReadUrl, final String messageId)
    {
        Intent intent = new Intent(context, NewCameraActivity.class);
        intent.putExtra(MESSAGE_READ_URL_EXTRA, messageReadUrl);
        intent.putExtra(MESSAGE_ID, messageId);
        context.startActivity(intent);
    }
}
