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
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.ui.CroppingLayout;
import com.chatwala.android.util.FutureCallback;
import com.chatwala.android.util.Logger;

import java.util.concurrent.FutureTask;

/**
 * Created by Eliezer on 4/23/2014.
 */
public class ConversationStarterFragment extends ChatwalaFragment implements TextureView.SurfaceTextureListener {
    private CWCamera camera;
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
    public void onCameraReady(CWCamera camera) {
        Logger.i("Camera is ready for ConversationStarterFragment");
        this.camera = camera;
    }

    @Override
    public void onActionButtonClicked() {
        if(camera.isRecording()) {
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
        if(camera != null && !camera.hasError()) {
            if(camera.toggleCamera()) {
                camera.attachToPreview(recordingSurface.getSurfaceTexture());
            }
        }
    }

    @Override
    protected void onBottomFragmentClicked() {
        if(!camera.isRecording()) {
            startRecording(false);
        }
    }

    private void startRecording(boolean fromButtonPress) {
        if(!getCwActivity().startRecording(10000)) {
            Toast.makeText(getActivity(), "Couldn't start recording", Toast.LENGTH_LONG).show();
        }
        else {
            actionImage.setImageResource(R.drawable.record_stop);
            countdownTimer = new CountDownTimer(11000, 1000) {
                @Override
                public void onTick(long tick) {
                    tick /= 1000 + 1;

                    int res = R.string.recording_countdown;
                    if(tick <= 5) {
                        res = R.string.sending_reply_countdown;
                    }
                    bottomText.setText(getString(res, tick));
                }

                @Override
                public void onFinish() {}
            }.start();
        }
    }

    @Override
    protected void onRecordingFinished(final RecordingInfo recordingInfo) {
        bottomText.setText(R.string.basic_instructions);
        actionImage.setImageResource(R.drawable.record_circle);

        if(true) {
            MessageManager.getInstance().getMessageVideoMetadata(recordingInfo.getRecordingFile(), new FutureCallback<VideoMetadata>() {
                @Override
                public void runOnMainThread(FutureTask<VideoMetadata> future) {
                    if (!future.isCancelled()) {
                        getCwActivity().showPreview(future);
                    }
                }
            });
        }
        else {
            MessageManager.getInstance().sendUnknownRecipientMessage(recordingInfo.getRecordingFile());
        }
    }

    private void attachCamera(final SurfaceTexture surfaceTexture) {
        if(camera == null) {
            recordingSurface.postDelayed(new Runnable() {
                @Override
                public void run() {
                    attachCamera(surfaceTexture);
                }
            }, 500);
            return;
        }

        camera.attachToPreview(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        Logger.i("Surface is ready for ConversationStarterFragment");
        attachCamera(surfaceTexture);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if(camera != null) {
            if (camera.isRecording()) {
                camera.stopRecording(true);
            }
            if(camera.isShowingPreview()) {
                camera.stopPreview();
            }
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {}
}
