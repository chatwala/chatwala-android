package co.touchlab.customcamera;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import co.touchlab.customcamera.ui.TimerDial;
import co.touchlab.customcamera.util.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.*;
import java.net.URLEncoder;
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

    public enum AppState
    {
        Off, Transition, LoadingFileCamera, ReadyStopped, PlaybackOnly, /*PlaybackRecording,*/ RecordingLimbo, Recording, PreviewLoading, PreviewReady
    }

    // ************* onCreate only *************
    private CroppingLayout cameraPreviewContainer, videoViewContainer;
    private View topFrameMessage;
    private TextView topFrameMessageText;
    private View bottomFrameMessage;
    private TextView bottomFrameMessageText;
    private DynamicTextureVideoView messageVideoView;
    private DynamicVideoThumbImageView messageVideoThumbnailView;
    private DynamicTextureVideoView recordPreviewVideoView;
    private View closeRecordPreviewView;
    private ImageView timerKnob;
    private TimerDial timerDial;
    // ************* onCreate only *************

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
            case ReadyStopped:
                timerKnob.setVisibility(View.VISIBLE);
                timerKnob.setImageResource(R.drawable.record_circle);
                break;
            case PlaybackOnly:
            case Recording:
                timerKnob.setVisibility(View.VISIBLE);
                timerKnob.setImageResource(R.drawable.record_stop);
                break;
            case PreviewReady:
                timerKnob.setVisibility(View.VISIBLE);
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

        ChatwalaApplication application = (ChatwalaApplication) getApplication();
        if(!application.isSplashRan())
        {
            application.setSplashRan(true);
            final View splash = getLayoutInflater().inflate(R.layout.splash_ripple, null);
            final ViewGroup root = (ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content);
            root.addView(splash);
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    splash.startAnimation(AnimationUtils.loadAnimation(NewCameraActivity.this, R.anim.splash_fade_out));
                    root.removeView(splash);
                    View message = getLayoutInflater().inflate(R.layout.message_dialag, null);
                    TextView messageText = (TextView) message.findViewById(R.id.message_dialag_text);
                    messageText.setTypeface(((ChatwalaApplication) getApplication()).fontMd);
                    messageText.setText("I like messages!");
                    root.addView(message);
                }
            }, 3000);
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
        topFrameMessageText = (TextView)findViewById(R.id.topFrameMessageText);
        Typeface fontDemi = ((ChatwalaApplication) getApplication()).fontMd;
        topFrameMessageText.setTypeface(fontDemi);
        bottomFrameMessage = findViewById(R.id.bottomFrameMessage);
        bottomFrameMessageText = (TextView)findViewById(R.id.bottomFrameMessageText);
        bottomFrameMessageText.setTypeface(fontDemi);

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
            prepareEmail(recordPreviewFile, chatMessage, chatMessageVideoMetadata);
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

        hideMessage(topFrameMessage);

        setAppState(AppState.RecordingLimbo);
        if (messageVideoView != null)
            messageVideoView.pause();
        cameraPreviewView.startRecording();
    }

    private void stopRecording()
    {
        AndroidUtils.isMainThread();
        setAppState(AppState.PreviewLoading);
        hideMessage(bottomFrameMessage);
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
            messageVideoView = new DynamicTextureVideoView(NewCameraActivity.this, chatMessageVideoMetadata.videoFile, chatMessageVideoMetadata.width, chatMessageVideoMetadata.height, chatMessageVideoMetadata.rotation);
            videoViewContainer.addView(messageVideoView);
            messageVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    showMessage(bottomFrameMessage, bottomFrameMessageText, R.color.message_background_alpha, R.string.now_record_reply);
                }
            });
            showMessage(topFrameMessage, topFrameMessageText, R.color.message_background_alpha, R.string.play_message_record_reaction);
            messageVideoThumbnailView = new DynamicVideoThumbImageView(NewCameraActivity.this, chatMessageVideoMetadata.width, chatMessageVideoMetadata.height);
            videoViewContainer.addView(messageVideoThumbnailView);
            messageVideoView.setVisibility(View.GONE);
            messageVideoThumbnailView.setVisibility(View.VISIBLE);
            messageVideoThumbnailView.setImageBitmap(chatMessageVideoBitmap);
        }
        else
        {
            showMessage(bottomFrameMessage, bottomFrameMessageText, R.color.message_background_clear, R.string.basic_instructions);
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
            recordPreviewVideoView = new DynamicTextureVideoView(NewCameraActivity.this, recordPreviewFile, videoInfo.width, videoInfo.height, videoInfo.rotation);

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
        recordPreviewVideoView.setOnCompletionListener(null);
        cameraPreviewContainer.removeView(recordPreviewVideoView);
        findViewById(R.id.recordPreviewClick).setOnClickListener(null);
        closeRecordPreviewView.setVisibility(View.GONE);

        createSurface();
    }

    private void tearDownSurface()
    {
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
        messageVideoThumbnailView = null;
        chatMessageVideoBitmap = null;
        findViewById(R.id.recordPreviewClick).setOnClickListener(null);
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

                String uriText =
                        "mailto:" +
                                sendTo +
                                "?subject=" + URLEncoder.encode("subject") +
                                "&body=" + URLEncoder.encode("body");

                Uri mailtoUri = Uri.parse(uriText);
                Uri dataUri = Uri.fromFile(outZip);

                if (AppPrefs.getInstance(NewCameraActivity.this).getPrefSelectedEmail() != null)
                {
                    Intent gmailIntent = new Intent();
                    gmailIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                    gmailIntent.setData(mailtoUri);

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
