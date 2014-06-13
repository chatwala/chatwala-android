package com.chatwala.android.camera;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.chatwala.android.R;
import com.chatwala.android.media.CwTrack;
import com.chatwala.android.media.CwVideoTrack;
import com.chatwala.android.media.OnTrackReadyListener;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.ui.CroppingLayout;
import com.chatwala.android.util.CwAnalytics;
import com.chatwala.android.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConversationReplierFragment extends ChatwalaFragment implements TextureView.SurfaceTextureListener,
        OnTrackReadyListener, AudioManager.OnAudioFocusChangeListener {
    private ChatwalaRecordingTexture recordingSurface;
    private ChatwalaPlaybackTexture playbackSurface;
    private ChatwalaMessage replyingToMessage;
    private Future<VideoMetadata> playbackMetadataFuture;
    private Future<VideoMetadata> previewMetadataFuture;
    private VideoMetadata playbackMetadata;
    private File recordedFile;
    private ImageView actionImage;
    private TextView topText;
    private TextView bottomText;
    private CountDownTimer countdownTimer;
    private long reviewStartTime;

    private Runnable startRecordingDelayedRunnable = new Runnable() {
        @Override
        public void run() {
            removeViewFromTop(topText);
            getCwActivity().disableActionButton(500);
            CwAnalytics.sendReviewCompletedEvent(CwAnalytics.Initiator.AUTO, getReviewDuration());
            startRecording(CwAnalytics.Initiator.AUTO);
        }
    };

    private ReplierState fragState = ReplierState.PREP;
    private ReplierState previousRecordState = ReplierState.PREP;

    /*package*/ enum ReplierState {
        PREP, READY, REVIEW, REACT, REPLY
    }

    public ConversationReplierFragment() {}

    public static ConversationReplierFragment newInstance(ChatwalaMessage message) {
        ConversationReplierFragment frag = new ConversationReplierFragment();
        Bundle args = new Bundle();
        args.putParcelable("message", message);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        CwAnalytics.setAnalyticsCategory(CwAnalytics.AnalyticsCategoryType.CONVERSATION_REPLIER);

        replyingToMessage = getArguments().getParcelable("message");
        if(replyingToMessage == null) {
            Toast.makeText(getActivity(), "Could not load message", Toast.LENGTH_LONG).show();
            getCwActivity().showConversationStarter();
            return;
        }

        MessageManager.markMessageAsRead(replyingToMessage);

        final CroppingLayout topCl = new CroppingLayout(getActivity());
        recordingSurface = new ChatwalaRecordingTexture(getActivity());
        recordingSurface.setSurfaceTextureListener(this);
        topCl.addView(recordingSurface);
        setTopView(topCl);
        topText = generateCwTextView(R.string.play_message_record_reaction, Color.argb(125, 255, 255, 255));
        addViewToTop(topText, false);

        actionImage = new ImageView(getActivity());
        actionImage.setImageResource(R.drawable.ic_action_playback_play);
        getCwActivity().setActionView(actionImage);

        File playbackFile = replyingToMessage.getLocalVideoFile();
        if(playbackFile == null || !playbackFile.exists()) {
            Toast.makeText(getActivity(), "Could not load video.", Toast.LENGTH_LONG).show();
            getCwActivity().showConversationStarter();
            return;
        }
        playbackMetadataFuture = MessageManager.getMessageVideoMetadata(playbackFile);

        post(new Runnable() {
            @Override
            public void run() {
                if(playbackMetadataFuture != null && playbackMetadataFuture.isDone()) {
                    if(!playbackMetadataFuture.isCancelled()) {
                        try {
                            onVideoMetadataReady(playbackMetadataFuture.get());
                            playbackMetadataFuture = null;
                        }
                        catch(Exception e) {
                            Logger.e("There was an error getting the playback metadata", e);
                            Toast.makeText(getActivity(), "Could not load video", Toast.LENGTH_LONG).show();
                            getCwActivity().showConversationStarter();
                        }
                    }
                }
                else {
                    postDelayed(this, 750);
                }
            }
        });
    }

    private void onVideoMetadataReady(VideoMetadata playbackMetadata) {
        try {
            this.playbackMetadata = playbackMetadata;
            if (playbackMetadata != null) {
                final CroppingLayout bottomCl = new CroppingLayout(getActivity());
                playbackSurface = new ChatwalaPlaybackTexture(getActivity(), new CwVideoTrack(playbackMetadata), this);
                bottomText = generateCwTextView("", Color.argb(125, 255, 255, 255));
                bottomCl.addView(playbackSurface);
                setBottomView(bottomCl);
            }
            else {
                Toast.makeText(getActivity(), "Could not load video", Toast.LENGTH_LONG).show();
                getCwActivity().showConversationStarter();
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Could not load video", Toast.LENGTH_LONG).show();
            getCwActivity().showConversationStarter();
        }
    }

    @Override
    public boolean onInitialTrackReady(CwTrack track) {
        fragState = ReplierState.READY;
        playbackSurface.seekTo(100);
        if(getCwActivity().isMessageLoadingTimerShowing()) {
            getCwActivity().fadeAndHideMessageLoadingTimer();
        }
        getCwActivity().setBurgerEnabled(getCwActivity().wasFirstButtonPressed());
        return false;
    }

    @Override
    public boolean onTrackReady(CwTrack track) {
        return true;
    }

    @Override
    public boolean onTracksFinished() {
        if(fragState != ReplierState.REACT) {
            return false;
        }
        fragState = ReplierState.REPLY;
        getCwActivity().disableActionButton(500);
        addViewToBottom(bottomText, false);
        countdownTimer = new CountDownTimer(11000, 1000) {
            @Override
            public void onTick(long tick) {
                tick /= 1000 + 1;

                int res = R.string.recording_reply_countdown;
                if(tick <= 5) {
                    res = R.string.sending_reply_countdown;
                }
                bottomText.setText(getString(res, tick));
            }

            @Override
            public void onFinish() {}
        }.start();
        CwAnalytics.sendRecordingStopEvent(CwAnalytics.Initiator.AUTO, CwAnalytics.ACTION_COMPLETE_REACTION,
                getCwActivity().getCurrentRecordingDuration());
        CwAnalytics.sendRecordingStartEvent(CwAnalytics.Initiator.AUTO, CwAnalytics.ACTION_START_RECORDING);
        return false;
    }

    @Override
    public void onTrackError() {
        Toast.makeText(getActivity(), "There was an error playing back this message.", Toast.LENGTH_SHORT).show();
        getCwActivity().showConversationStarter();
    }

    @Override
    public boolean onBackPressed() {
        setWasBackButtonPressed(true);
        getCwActivity().showConversationStarter();
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        abandonAudioFocus();

        if(startRecordingDelayedRunnable != null && playbackSurface != null) {
            playbackSurface.removeCallbacks(startRecordingDelayedRunnable);
        }
        if(countdownTimer != null) {
            countdownTimer.cancel();
        }
        if(playbackMetadataFuture != null) {
            playbackMetadataFuture.cancel(true);
        }
        if(previewMetadataFuture != null) {
            previewMetadataFuture.cancel(true);
        }
    }

    private void resetUi() {
        abandonAudioFocus();
        if(startRecordingDelayedRunnable != null) {
            playbackSurface.removeCallbacks(startRecordingDelayedRunnable);
        }
        if(countdownTimer != null) {
            countdownTimer.cancel();
        }

        removeViewFromTop(topText);
        removeViewFromBottom(bottomText);

        topText = generateCwTextView(R.string.play_message_record_reaction, Color.argb(125, 255, 255, 255));
        addViewToTop(topText, false);
        playbackSurface.reset();
        try {
            playbackSurface.init(new CwVideoTrack(playbackMetadata), this);
        } catch (IOException e) {
            Logger.e("Couldn't reset playback surface. Starting conversation starter");
            getCwActivity().showConversationStarter();
            return;
        }
        playbackSurface.seekTo(100);

        actionImage.setImageResource(R.drawable.ic_action_playback_play);

        getCwActivity().setBurgerEnabled(getCwActivity().wasFirstButtonPressed());
        fragState = ReplierState.READY;
    }

    @Override
    public void onActionButtonClicked() {
        if(playbackSurface.isPlaying() || getCwActivity().isRecording()) {
            previousRecordState = fragState;
            if(getCwActivity().isRecording()) {
                if(fragState == ReplierState.REACT) {
                    getCwActivity().stopRecording(CwAnalytics.Initiator.BUTTON, CwAnalytics.ACTION_CANCEL_REACTION);
                }
                else {
                    getCwActivity().stopRecording();
                }
            }
            else {
                CwAnalytics.sendReviewCancelledEvent(CwAnalytics.Initiator.BUTTON, getReviewDuration());
            }
            resetUi();
        }
        else {
            startFlow(CwAnalytics.Initiator.BUTTON);
        }
    }

    private void startFlow(CwAnalytics.Initiator initiator) {
        //TODO this needs to be handled more gracefully...does it ever have to be handled?
        if(fragState != ReplierState.READY) {
            return;
        }

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getAudioFocus();

        //we're in a ready state, start'er up
        actionImage.setImageResource(R.drawable.record_stop);
        removeViewFromTop(topText);
        playbackSurface.seekTo(0);
        if(replyingToMessage.getStartRecording() == 0) {
            startRecording(initiator);
        }
        else {
            topText.setText("");
            addViewToTop(topText, false);
            getCwActivity().setBurgerEnabled(false);
            playbackSurface.postDelayed(startRecordingDelayedRunnable, (long) replyingToMessage.getStartRecording() * 1000);
            fragState = ReplierState.REVIEW;
            reviewStartTime = System.currentTimeMillis();
            CwAnalytics.sendReviewStartedEvent(initiator);
        }
        playbackSurface.start();
    }

    private void startRecording(final CwAnalytics.Initiator initiator) {
        getCwActivity().startRecording((playbackMetadata.getDuration() - ((long) replyingToMessage.getStartRecording()) * 1000) + 10000,
                initiator, CwAnalytics.ACTION_START_REACTION, CwAnalytics.ACTION_COMPLETE_RECORDING,
                new ChatwalaActivity.OnRecordingFinishedListener() {
            @Override
            public void onRecordingFinished(RecordingInfo recordingInfo) {
                recordedFile = recordingInfo.getRecordingFile();
                if(recordingInfo.wasManuallyStopped()) {
                    if(previousRecordState == ReplierState.REPLY) {
                        if(shouldShowPreview()) {
                            showPreview(recordingInfo);
                            CwAnalytics.sendRecordingStopEvent(initiator, CwAnalytics.ACTION_COMPLETE_RECORDING, recordingInfo.getRecordingLength());
                        }
                        else {
                            ShouldSendRecordingDialog frag = ShouldSendRecordingDialog.newInstance(recordingInfo.getRecordingLength());
                            frag.setTargetFragment(ConversationReplierFragment.this, 0);
                            frag.show(getFragmentManager(), "dialog");
                        }
                    }
                    else {
                        recordedFile.delete();
                        recordedFile = null;
                    }
                }
                else {
                    if(shouldShowPreview()) {
                        showPreview(recordingInfo);
                    }
                    else {
                        sendReply();
                    }
                }
            }
        });
        fragState = ReplierState.REACT;
    }

    private void showPreview(RecordingInfo recordingInfo) {
        previewMetadataFuture = MessageManager.getMessageVideoMetadata(recordingInfo.getRecordingFile());
        post(new Runnable() {
            @Override
            public void run() {
                if(previewMetadataFuture != null && previewMetadataFuture.isDone()) {
                    if(!previewMetadataFuture.isCancelled()) {
                        try {
                            getCwActivity().showPreviewForReplier(previewMetadataFuture.get(), replyingToMessage);
                        } catch (Exception e) {
                            //TODO need better message
                            Toast.makeText(getActivity(), "There was an error recording your message. Please try again.", Toast.LENGTH_SHORT).show();
                            getCwActivity().showConversationStarter();
                        }
                    }
                }
                else {
                    postDelayed(this, 750);
                }
            }
        });

    }

    private void sendReply() {
        if(recordedFile != null) {
            MessageManager.startSendReplyMessage(replyingToMessage, recordedFile);
            Toast.makeText(getActivity(), "Message sent.", Toast.LENGTH_SHORT).show();
        }
        getCwActivity().showConversationStarter();
    }

    private long getReviewDuration() {
        return System.currentTimeMillis() - reviewStartTime;
    }

    @Override
    protected void onTopFragmentClicked() {

    }

    @Override
    protected void onBottomFragmentClicked() {
        if(fragState == ReplierState.READY) {
            startFlow(CwAnalytics.Initiator.SCREEN);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        Logger.i("Surface is ready for ConversationReplierFragment");
        getCwActivity().setPreviewForCamera(recordingSurface);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (getCwActivity().isRecording()) {
            CwAnalytics.Initiator initiator;
            if(wasBackButtonPressed()) {
                initiator = CwAnalytics.Initiator.BACK;
            }
            else {
                initiator = CwAnalytics.Initiator.ENVIRONMENT;
            }
            getCwActivity().stopRecording(initiator, getAnalyticsCancelledAction());
        }
        if(getCwActivity().isShowingCameraPreview()) {
            getCwActivity().stopCameraPreview();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {}
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}

    private void getAudioFocus() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Logger.w("Couldn't get audio focus");
        }
    }

    private void abandonAudioFocus() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if(playbackSurface == null) {
            Logger.w("Couldn't init media player for when we got audio focus...abandoning audio focus");
            abandonAudioFocus();
            return;
        }

        switch(focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                playbackSurface.setVolume(1f, 1f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                playbackSurface.mute();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                playbackSurface.setVolume(.5f, .5f);
                break;
            default:
                playbackSurface.setVolume(1f, 1f);
        }
    }

    private String getAnalyticsCancelledAction() {
        if(getCwActivity().isRecording()) {
            if(fragState == ReplierState.REACT) {
                return CwAnalytics.ACTION_CANCEL_REACTION;
            }
            else {
                return CwAnalytics.ACTION_CANCEL_RECORDING;
            }
        }
        else if(fragState == ReplierState.REVIEW) {
            return CwAnalytics.ACTION_CANCEL_REVIEW;
        }
        else {
            return null;
        }
    }

    public static class ShouldSendRecordingDialog extends DialogFragment {
        public ShouldSendRecordingDialog() {}

        public static ShouldSendRecordingDialog newInstance(long recordingLength) {
            ShouldSendRecordingDialog frag = new ShouldSendRecordingDialog();
            Bundle args = new Bundle();
            args.putLong("recordingLength", recordingLength);
            frag.setArguments(args);
            return frag;
        }

        private ConversationReplierFragment getReplierFragment() {
            return ((ConversationReplierFragment) getTargetFragment());
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final long recordingLength = getArguments().getLong("recordingLength");

            AlertDialog.Builder builder;
            Dialog d;

            builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.message_send_or_cancel_title)
                    .setCancelable(false)
                    .setPositiveButton(R.string.message_send_or_cancel_send, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            getReplierFragment().sendReply();
                            CwAnalytics.sendRecordingStopEvent(CwAnalytics.Initiator.BUTTON, CwAnalytics.ACTION_COMPLETE_RECORDING,
                                    recordingLength);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.message_send_or_cancel_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            getReplierFragment().recordedFile.delete();
                            getReplierFragment().recordedFile = null;
                            CwAnalytics.sendRecordingStopEvent(CwAnalytics.Initiator.BUTTON, CwAnalytics.ACTION_CANCEL_RECORDING,
                                    recordingLength);
                            dialog.dismiss();
                        }
                    });
            setCancelable(false);
            d = builder.create();
            return d;
        }
    }
}
