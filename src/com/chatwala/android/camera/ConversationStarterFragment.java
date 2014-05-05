package com.chatwala.android.camera;

import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.chatwala.android.R;
import com.chatwala.android.camera.ChatwalaActivity.OnRecordingFinishedListener;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.ui.CroppingLayout;
import com.chatwala.android.util.FutureCallback;
import com.chatwala.android.util.Logger;

import java.util.concurrent.FutureTask;

/**
 * Created by Eliezer on 4/23/2014.
 */
public class ConversationStarterFragment extends ChatwalaFragment implements TextureView.SurfaceTextureListener {
    private ChatwalaRecordingTexture recordingSurface;
    private ImageView actionImage;
    private TextView bottomText;
    private CountDownTimer countdownTimer;

    public ConversationStarterFragment() {}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final CroppingLayout cl = new CroppingLayout(getActivity());
        recordingSurface = new ChatwalaRecordingTexture(getActivity());
        recordingSurface.setSurfaceTextureListener(this);
        cl.addView(recordingSurface);
        setTopView(cl);

        actionImage = new ImageView(getActivity());
        actionImage.setImageResource(R.drawable.record_circle);
        getCwActivity().setActionView(actionImage);

        bottomText = generateCwTextView(R.string.basic_instructions, Color.TRANSPARENT);
        setBottomView(bottomText);
    }

    @Override
    public void onPause() {
        super.onPause();

        if(countdownTimer != null) {
            countdownTimer.cancel();
        }
    }

    @Override
    public void onActionButtonClicked() {
        if(getCwActivity().isRecording()) {
            getCwActivity().stopRecording();
            if(countdownTimer != null) {
                countdownTimer.cancel();
                countdownTimer = null;
            }
        }
        else {
            startRecording(true);
        }
    }

    @Override
    protected void onTopFragmentClicked() {
        if(getCwActivity().toggleCamera()) {
            getCwActivity().setPreviewForCamera(recordingSurface);
        }
    }

    @Override
    protected void onBottomFragmentClicked() {
        if(!getCwActivity().isRecording()) {
            startRecording(false);
        }
    }

    private void startRecording(boolean fromButtonPress) {
        if(!getCwActivity().startRecording(10000, recordingFinishedListener)) {
            Toast.makeText(getActivity(), "Couldn't start recording", Toast.LENGTH_LONG).show();
        }
        else {
            actionImage.setImageResource(R.drawable.record_stop);
            countdownTimer = new CountDownTimer(11000, 1000) {
                @Override
                public void onTick(long tick) {
                    tick /= 1000 + 1;
                    bottomText.setText(getString(R.string.recording_countdown, tick));
                }

                @Override
                public void onFinish() {}
            }.start();
        }
    }

    private OnRecordingFinishedListener recordingFinishedListener = new OnRecordingFinishedListener() {
        @Override
        public void onRecordingFinished(RecordingInfo recordingInfo) {
            bottomText.setText(R.string.basic_instructions);
            actionImage.setImageResource(R.drawable.record_circle);

            if(true) {
                MessageManager.getInstance().getMessageVideoMetadata(recordingInfo.getRecordingFile(), new FutureCallback<VideoMetadata>() {
                    @Override
                    public void runOnMainThread(FutureTask<VideoMetadata> future) {
                        if (!future.isCancelled()) {
                            getCwActivity().showPreview(future, false);
                        }
                    }
                });
            }
            else {
                MessageManager.getInstance().sendUnknownRecipientMessage(recordingInfo.getRecordingFile());
            }
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
            getCwActivity().stopRecording(true);
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
