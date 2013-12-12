package com.chatwala.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.chatwala.android.ui.TimerDial;
import com.chatwala.android.util.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.*;
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
public class NewCameraActivity extends Activity
{
    public static final int RECORDING_TIME = 10000;
    private ViewGroup blueMessageDialag;
    private View splash;

    public enum AppState
    {
        Off, Transition, LoadingFileCamera, ReadyStopped, PlaybackOnly, PlaybackRecording, RecordingLimbo, Recording, PreviewLoading, PreviewReady
    }

    // ************* onCreate only *************
    private CroppingLayout cameraPreviewContainer, videoViewContainer;
    private View topFrameMessage;
    private TextView topFrameMessageText;
    private View bottomFrameMessage;
    private TextView bottomFrameMessageText;
    private DynamicVideoView messageVideoView;
    private DynamicVideoView recordPreviewVideoView;
    private View closeRecordPreviewView;
    private ImageView timerKnob;
    private TimerDial timerDial;
    // ************* onCreate only *************

    // ************* DANGEROUS STATE *************
    private CameraPreviewView cameraPreviewView;
    private ChatMessage chatMessage;
    private VideoUtils.VideoMetadata chatMessageVideoMetadata;
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
        CWLog.b(NewCameraActivity.class, "setAppState: " + appState + " (" + System.currentTimeMillis() + ")");
        AndroidUtils.isMainThread();
        this.appState = appState;

        switch (this.appState)
        {
            case ReadyStopped:
                timerKnob.setVisibility(View.VISIBLE);
                if(chatMessage != null)
                    timerKnob.setImageResource(R.drawable.ic_action_playback_play);
                else
                    timerKnob.setImageResource(R.drawable.record_circle);
                break;
            case PlaybackOnly:
            case PlaybackRecording:
            case Recording:
                timerKnob.setVisibility(View.VISIBLE);
                timerKnob.setImageResource(R.drawable.record_stop);
                break;
            case PreviewReady:
                timerKnob.setVisibility(View.VISIBLE);
                timerKnob.setImageResource(R.drawable.ic_action_send_ios);
                break;
            default:
                timerKnob.setVisibility(View.INVISIBLE);
        }
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
        if(blueMessageDialag != null)
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
        CWLog.b(NewCameraActivity.class, "onCreate start");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.crop_test);

        ChatwalaApplication application = (ChatwalaApplication) getApplication();
        if (!application.isSplashRan())
        {
//            runWaterSplash(application);
        }

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
                closeResultPreview();
            }
        });
        CWLog.b(NewCameraActivity.class, "onCreate end");
    }

    private void runWaterSplash()
    {
        splash = getLayoutInflater().inflate(R.layout.splash_ripple, null);
        final ViewGroup root = findViewRoot();
        root.addView(splash);
        /*new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                splash.startAnimation(AnimationUtils.loadAnimation(NewCameraActivity.this, R.anim.splash_fade_out));
                root.removeView(splash);
                *//*showDialag(R.string.play_message_record_reaction, new DialagButton(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Toast.makeText(NewCameraActivity.this, "Heyo", Toast.LENGTH_LONG).show();
                    }
                }, R.string.sure), new DialagButton(new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View v)
                                    {
                                        Toast.makeText(NewCameraActivity.this, "Heyo", Toast.LENGTH_LONG).show();
                                    }
                                }, R.string.sure));*//*
            }
        }, 3000);*/
        CWLog.b(NewCameraActivity.class, "runWaterSplash end");
    }

    private void removeWaterSplash()
    {
        if(splash != null)
        {
            final ViewGroup root = findViewRoot();
            splash.startAnimation(AnimationUtils.loadAnimation(NewCameraActivity.this, R.anim.splash_fade_out));
            root.removeView(splash);
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

        setAppState(AppState.Transition);

        Uri uri = getIntent().getData();
        if(uri != null)
        {
            runWaterSplash();
        }

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
        AppState state = getAppState();
        CWLog.userAction(NewCameraActivity.class, "Timer button pressed in state: " + state.name());

        //Don't do anything.  These should be very short states.
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

        if (state == AppState.PlaybackRecording)
        {
            abortRecording();
            return;
        }

        if (state == AppState.Recording)
        {
            stopRecording();
            return;
        }

        if (state == AppState.PreviewReady)
        {
            prepareEmail(recordPreviewFile, chatMessage, chatMessageVideoMetadata);
            closeResultPreview();
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
            this.startRecordingTime = startRecordingTime;
            this.recordingStarted = recordingStarted;
            this.messageVideoDuration = messageVideoDuration;
            endRecordingTime = messageVideoDuration + RECORDING_TIME;
        }

        public void abort()
        {
            CWLog.b(NewCameraActivity.class, "HeartbeatTimer:abort");
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
        CWLog.b(NewCameraActivity.class, "startPlaybackRecording");
        AndroidUtils.isMainThread();

        hideMessage(topFrameMessage);
        int recordingStartMillis = chatMessage == null ? 0 : (int) Math.round(chatMessage.metadata.startRecording * 1000);
        if (messageVideoView != null)
        {
            setAppState(AppState.PlaybackOnly);
            messageVideoView.start();
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

        if(chatMessage == null)
            hideMessage(bottomFrameMessage);

        setAppState(AppState.RecordingLimbo);
        if (messageVideoView != null)
            messageVideoView.pause();
        cameraPreviewView.startRecording();
    }

    private void stopRecording()
    {
        CWLog.b(NewCameraActivity.class, "stopRecording");
        AndroidUtils.isMainThread();
        setAppState(AppState.PreviewLoading);
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

        new MessageLoaderTask().execute();
    }

    private void messageLoaded(ChatMessage message)
    {
        CWLog.b(NewCameraActivity.class, "messageLoaded");
        this.chatMessage = message;

        if (chatMessage != null)
        {
            messageVideoView = new DynamicVideoView(NewCameraActivity.this, chatMessageVideoMetadata.videoFile, chatMessageVideoMetadata.width, chatMessageVideoMetadata.height, new DynamicVideoView.VideoReadyCallback()
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
            recordPreviewVideoView = new DynamicVideoView(NewCameraActivity.this, recordPreviewFile, videoInfo.width, videoInfo.height, null, false);

            cameraPreviewContainer.addView(recordPreviewVideoView);
            closeRecordPreviewView.setVisibility(View.VISIBLE);
            recordPreviewVideoView.start();
            recordPreviewVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    recordPreviewVideoView.seekTo(0);
                    recordPreviewVideoView.start();
                }
            });

            setAppState(AppState.PreviewReady);
        }
    }

    private void createSurface()
    {
        AndroidUtils.isMainThread();
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
            ChatMessage cm = ShareUtils.extractFileAttachment(NewCameraActivity.this);
            if (cm != null)
            {
                try
                {
                    chatMessageVideoMetadata = VideoUtils.findMetadata(cm.messageVideo);
//                    chatMessageVideoBitmap = VideoUtils.createVideoFrame(cm.messageVideo.getPath(), 0l);
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
        new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object... params)
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
                    long chatMessageDuration = originalVideoMetadata == null ? 0 : originalVideoMetadata.duration;
                    sendMessageMetadata.startRecording = ((double) Math.max(chatMessageDuration - startRecordingMillis, 0)) / 1000d;

                    String myEmail = AppPrefs.getInstance(NewCameraActivity.this).getPrefSelectedEmail();

                    if (myEmail == null && originalMessage != null)
                        myEmail = originalMessage.probableEmailSource;

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

                String sendTo = "";
                if (originalMessage != null && originalMessage.metadata.senderId != null)
                    sendTo = originalMessage.metadata.senderId.trim();

                //Not sure how this would creep in there, but deal with it.
                if(sendTo.equalsIgnoreCase("null"))
                    sendTo = "";

                String uriText = "mailto:" + sendTo;

                Uri mailtoUri = Uri.parse(uriText);
                Uri dataUri = Uri.fromFile(outZip);

                if (AppPrefs.getInstance(NewCameraActivity.this).getPrefSelectedEmail() != null)
                {
                    Intent gmailIntent = new Intent();
                    gmailIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                    gmailIntent.setData(mailtoUri);
                    gmailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.message_subject));
                    gmailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Chatwala is a new way to have real conversations with friends. <a href=\"http://www.chatwala.com\">Get the App</a>."));

                    gmailIntent.putExtra(Intent.EXTRA_STREAM, dataUri);

                    try
                    {
                        startActivity(gmailIntent);
                    }
                    catch (ActivityNotFoundException ex)
                    {
                        // handle error
                    }
                }
                else
                {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);

                    intent.setData(mailtoUri);
                    intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.message_subject));
                    intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Chatwala is a new way to have real conversations with friends. <a href=\"http://www.chatwala.com\">Get the App</a>."));

                    intent.putExtra(Intent.EXTRA_STREAM, dataUri);
                    startActivity(Intent.createChooser(intent, "Send email..."));
                }

            }
        }.execute();
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
    }

    private void hideMessage(View messageView)
    {
        if (messageView.getVisibility() != View.VISIBLE)
            return;

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.message_fade_out);
        messageView.startAnimation(animation);
        messageView.setVisibility(View.GONE);
    }
}
