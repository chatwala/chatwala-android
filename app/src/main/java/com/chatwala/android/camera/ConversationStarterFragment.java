package com.chatwala.android.camera;

import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.chatwala.android.R;
import com.chatwala.android.camera.ChatwalaActivity.OnRecordingFinishedListener;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.messages.MessageStartInfo;
import com.chatwala.android.ui.CroppingLayout;
import com.chatwala.android.util.CwAnalytics;
import com.chatwala.android.util.Logger;

import java.io.File;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConversationStarterFragment extends ChatwalaFragment implements TextureView.SurfaceTextureListener {
    private ChatwalaRecordingTexture recordingSurface;
    private ImageView actionImage;
    private TextView bottomText;
    private CountDownTimer countdownTimer;
    private Future<VideoMetadata> previewMetadataFuture;

    private boolean disableAction = false;

    private Future<MessageStartInfo> messageStartInfoFuture;

    public ConversationStarterFragment() {}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        CwAnalytics.setAnalyticsCategory(CwAnalytics.AnalyticsCategoryType.CONVERSATION_STARTER);

        final CroppingLayout cl = new CroppingLayout(getActivity());
        recordingSurface = new ChatwalaRecordingTexture(getActivity());
        recordingSurface.setSurfaceTextureListener(this);
        cl.addView(recordingSurface);
        setTopView(cl);

        actionImage = new ImageView(getActivity());
        actionImage.setImageResource(R.drawable.record_circle);
        getCwActivity().setActionView(actionImage);

        if(getCwActivity().isTopContactsDeliveryMethod()) {
            countdownTimer = new CountDownTimer(4000, 1000) {
                @Override
                public void onTick(long tick) {
                    tick /= 1000;
                    bottomText.setText(getString(R.string.top_contacts_pre_recording_text, tick));
                }

                @Override
                public void onFinish() {
                    startRecording(CwAnalytics.Initiator.AUTO);
                }
            }.start();
        }

        bottomText = generateCwTextView(R.string.basic_instructions, Color.TRANSPARENT);
        setBottomView(bottomText);
    }

    @Override
    public boolean onBackPressed() {
        if(messageStartInfoFuture != null && !messageStartInfoFuture.isDone()) {
            messageStartInfoFuture.cancel(true);
            messageStartInfoFuture = null;
            return true;
        }
        else {
            setWasBackButtonPressed(true);
            return false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(countdownTimer != null) {
            countdownTimer.cancel();
        }
        if(messageStartInfoFuture != null && !messageStartInfoFuture.isDone()) {
            messageStartInfoFuture.cancel(true);
            messageStartInfoFuture = null;
        }
        if(previewMetadataFuture != null && !previewMetadataFuture.isDone()) {
            previewMetadataFuture.cancel(true);
        }
    }

    @Override
    public void onActionButtonClicked() {
        if(disableAction) {
            return;
        }

        if(getCwActivity().isRecording()) {
            getCwActivity().stopRecording(CwAnalytics.Initiator.BUTTON, CwAnalytics.ACTION_COMPLETE_RECORDING);
            if(countdownTimer != null) {
                countdownTimer.cancel();
                countdownTimer = null;
            }
        }
        else {
            startRecording(CwAnalytics.Initiator.BUTTON);
        }
    }

    @Override
    protected void onTopFragmentClicked() {
        if(disableAction) {
            return;
        }

        if(getCwActivity().toggleCamera()) {
            getCwActivity().setPreviewForCamera(recordingSurface);
        }
    }

    @Override
    protected void onBottomFragmentClicked() {
        if(disableAction) {
            return;
        }

        if(!getCwActivity().isRecording()) {
            startRecording(CwAnalytics.Initiator.SCREEN);
        }
    }

    private void startRecording(CwAnalytics.Initiator initiator) {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(countdownTimer != null) {
            countdownTimer.cancel();
        }
        messageStartInfoFuture = MessageManager.getNewMessageStartInfo();
        if(!getCwActivity().startRecording(10000, initiator, CwAnalytics.ACTION_START_RECORDING,
                CwAnalytics.ACTION_COMPLETE_RECORDING, recordingFinishedListener)) {
            messageStartInfoFuture.cancel(true);
            messageStartInfoFuture = null;
            Toast.makeText(getActivity(), "Couldn't start recording. Please try again.", Toast.LENGTH_SHORT).show();
        }
        else {
            actionImage.setImageResource(R.drawable.record_stop);
            countdownTimer = new CountDownTimer(11000, 1000) {
                @Override
                public void onTick(long tick) {
                    tick /= 1000;
                    if(getCwActivity().isTopContactsDeliveryMethod()) {
                        if(tick > 5) {
                            bottomText.setText(getString(R.string.top_contacts_recording_text, tick));
                        }
                        else {
                            bottomText.setText(getString(R.string.top_contacts_sending_text, tick));
                        }
                    }
                    else {
                        bottomText.setText(getString(R.string.recording_countdown, tick));
                    }
                }

                @Override
                public void onFinish() {}
            }.start();
        }
    }

    private OnRecordingFinishedListener recordingFinishedListener = new OnRecordingFinishedListener() {
        @Override
        public void onRecordingFinished(final RecordingInfo recordingInfo) {
            bottomText.setText(R.string.basic_instructions);
            actionImage.setImageResource(R.drawable.record_circle);

            disableAction = true;
            final ProgressBar pb = new ProgressBar(getActivity());
            pb.setIndeterminate(true);
            bottomText.setText("Sending message...");
            getCwActivity().setActionView(pb);

            pb.post(new Runnable() {
                @Override
                public void run() {
                    if (!messageStartInfoFuture.isDone()) {
                        pb.postDelayed(this, 1000);
                        return;
                    }

                    final MessageStartInfo info;
                    try {
                        if (messageStartInfoFuture.isCancelled()) {
                            Logger.w("The message start info request was cancelled");
                            bottomText.setText(R.string.basic_instructions);
                            Toast.makeText(getActivity(), "Couldn't contact server.  Please try again later.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        info = messageStartInfoFuture.get();

                        if (info == null) {
                            Logger.w("Didn't get the message start info back in time");
                            bottomText.setText(R.string.basic_instructions);
                            Toast.makeText(getActivity(), "Couldn't contact server.  Please try again later.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (shouldShowPreview()) {
                            previewMetadataFuture = MessageManager.getMessageVideoMetadata(recordingInfo.getRecordingFile());
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    if (previewMetadataFuture != null && previewMetadataFuture.isDone()) {
                                        if (!previewMetadataFuture.isCancelled()) {
                                            try {
                                                getCwActivity().showPreviewForStarter(previewMetadataFuture.get(), info);
                                            } catch (Exception e) {
                                                //TODO need better message
                                                Toast.makeText(getActivity(), "There was an error recording your message. Please try again.", Toast.LENGTH_SHORT).show();
                                                getCwActivity().showConversationStarter();
                                            }
                                        }
                                    } else {
                                        postDelayed(this, 750);
                                    }
                                }
                            });
                        } else {
                            File recordingFile = recordingInfo.getRecordingFile();
                            MessageManager.startSendUnknownRecipientMessage(recordingFile, info);
                            getCwActivity().sendShareUrl(info);
                        }
                    } catch (Exception e) {
                        Logger.w("Didn't get the message start info back in time");
                        bottomText.setText(R.string.basic_instructions);
                        Toast.makeText(getActivity(), "Couldn't contact server.  Please try again later.", Toast.LENGTH_LONG).show();
                    } finally {
                        disableAction = false;
                        getCwActivity().setActionView(actionImage);
                    }
                }
            });
        }
    };

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        Logger.i("Surface is ready for ConversationStarterFragment");
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
            getCwActivity().stopRecording(initiator, CwAnalytics.ACTION_CANCEL_RECORDING);
        }
        if(getCwActivity().isShowingCameraPreview()) {
            getCwActivity().stopCameraPreview();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {}
}
