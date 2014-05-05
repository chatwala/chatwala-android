package com.chatwala.android.camera;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.chatwala.android.R;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.ui.CroppingLayout;
import com.chatwala.android.util.FutureCallback;
import com.chatwala.android.util.Logger;

import java.io.File;
import java.util.concurrent.FutureTask;

/**
 * Created by Eliezer on 5/5/2014.
 */
public class ConversationReplierFragment extends ChatwalaFragment implements TextureView.SurfaceTextureListener {
    private ChatwalaRecordingTexture recordingSurface;
    private ChatwalaPlaybackTexture playbackSurface;
    private VideoMetadata playbackMetadata;
    private File recordedFile;
    private ImageView actionImage;
    private TextView topText;
    private TextView bottomText;
    private CountDownTimer countdownTimer;

    private String messageId;
    private ReplierState fragState = ReplierState.PREP;
    private ReplierState previousRecordState = ReplierState.PREP;

    /*package*/ enum ReplierState {
        PREP, READY, REVIEW, REACT, REPLY
    }

    public ConversationReplierFragment() {}

    public static ConversationReplierFragment newInstance(String messageId) {
        ConversationReplierFragment frag = new ConversationReplierFragment();
        Bundle args = new Bundle();
        args.putString("messageId", messageId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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

        File playbackFile = new File(Environment.getExternalStorageDirectory().getPath() + "/test.mp4");
        MessageManager.getInstance().getMessageVideoMetadata(playbackFile, new FutureCallback<VideoMetadata>() {
            @Override
            public void runOnMainThread(FutureTask<VideoMetadata> future) {
                if (!future.isCancelled()) {
                    try {
                        playbackMetadata = future.get();
                        if(playbackMetadata != null) {
                            final CroppingLayout bottomCl = new CroppingLayout(getActivity());
                            playbackSurface = new ChatwalaPlaybackTexture(getActivity(), playbackMetadata, false);
                            playbackSurface.setOnPlaybackReadyListener(new ChatwalaPlaybackTexture.OnPlaybackReadyListener() {
                                @Override
                                public void onPlaybackReady() {
                                    fragState = ReplierState.READY;
                                    playbackSurface.seekTo(100);
                                }
                            });
                            playbackSurface.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mediaPlayer) {
                                    if(fragState != ReplierState.REACT) {
                                        return;
                                    }
                                    fragState = ReplierState.REPLY;
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
                                }
                            });
                            bottomText = generateCwTextView("", Color.argb(125, 255, 255, 255));
                            bottomCl.addView(playbackSurface);
                            setBottomView(bottomCl);
                        }
                        else {
                            Toast.makeText(getActivity(), "Could not load video", Toast.LENGTH_LONG).show();
                            getCwActivity().showConversationStarter();
                        }
                    }
                    catch(Exception e) {
                        Toast.makeText(getActivity(), "Could not load video", Toast.LENGTH_LONG).show();
                        getCwActivity().showConversationStarter();
                    }
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        if(countdownTimer != null) {
            countdownTimer.cancel();
        }
    }

    private void resetUi() {
        if(countdownTimer != null) {
            countdownTimer.cancel();
        }
        if(getCwActivity().isRecording()) {
            getCwActivity().stopRecording();
            topText = generateCwTextView(R.string.play_message_record_reaction, Color.argb(125, 255, 255, 255));
            addViewToTop(topText, false);
        }
        playbackSurface.reset();
        actionImage.setImageResource(R.drawable.ic_action_playback_play);
        if(previousRecordState == ReplierState.REPLY) {
            removeViewFromBottom(bottomText);
        }
        fragState = ReplierState.READY;
    }

    @Override
    public void onActionButtonClicked() {
        if(playbackSurface.isPlaying() || getCwActivity().isRecording()) {
            previousRecordState = fragState;
            resetUi();
        }
        else { //we're in a ready state, start'er up
            actionImage.setImageResource(R.drawable.record_stop);
            removeViewFromTop(topText);
            playbackSurface.seekTo(0);
            playbackSurface.start();
            fragState = ReplierState.REACT;
            getCwActivity().startRecording(playbackMetadata.getDuration() + 10000, new ChatwalaActivity.OnRecordingFinishedListener() {
                @Override
                public void onRecordingFinished(RecordingInfo recordingInfo) {
                    recordedFile = recordingInfo.getRecordingFile();
                    if(recordingInfo.wasManuallyStopped()) {
                        if(previousRecordState == ReplierState.REPLY) {
                            if(false) {
                                showPreview(recordingInfo);
                            }
                            else {
                                ShouldSendRecordingDialog frag = new ShouldSendRecordingDialog();
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
                        if(true) {
                            showPreview(recordingInfo);
                        }
                        else {
                            sendReply();
                        }
                    }
                }
            });
        }
    }

    private void showPreview(RecordingInfo recordingInfo) {
        MessageManager.getInstance().getMessageVideoMetadata(recordingInfo.getRecordingFile(), new FutureCallback<VideoMetadata>() {
            @Override
            public void runOnMainThread(FutureTask<VideoMetadata> future) {
                if (!future.isCancelled()) {
                    getCwActivity().showPreview(future, true);
                }
            }
        });
    }

    private void sendReply() {
        if(recordedFile != null) {
            MessageManager.getInstance().sendReply(recordedFile);
        }
        getCwActivity().showConversationStarter();
    }

    @Override
    protected void onTopFragmentClicked() {

    }

    @Override
    protected void onBottomFragmentClicked() {
        if(fragState == ReplierState.READY) {
            onActionButtonClicked();
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
            getCwActivity().stopRecording(true);
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

    public static class ShouldSendRecordingDialog extends DialogFragment {
        public ShouldSendRecordingDialog() {}

        private ConversationReplierFragment getReplierFragment() {
            return ((ConversationReplierFragment) getTargetFragment());
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder;
            Dialog d;

            builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.send_or_cancel_title)
                    .setCancelable(false)
                    .setPositiveButton(R.string.send_or_cancel_send, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            getReplierFragment().sendReply();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.send_or_cancel_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            getReplierFragment().recordedFile.delete();
                            getReplierFragment().recordedFile = null;
                            dialog.dismiss();
                        }
                    });
            setCancelable(false);
            d = builder.create();
            return d;
        }
    }
}
