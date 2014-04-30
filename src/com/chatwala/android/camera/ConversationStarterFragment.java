package com.chatwala.android.camera;

import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.chatwala.android.R;
import com.chatwala.android.util.Logger;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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

        recordingSurface = new ChatwalaRecordingTexture(getActivity());
        recordingSurface.setSurfaceTextureListener(this);
        addViewToTop(recordingSurface, false);
        recordingSurface.forceOnMeasure();

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
    protected void onRecordingFinished(File recordedFile) {
        bottomText.setText("Done");
        actionImage.setImageResource(R.drawable.record_circle);
        try {
            IOUtils.copy(new FileInputStream(recordedFile), new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/test.mp4"));
        }
        catch(Exception e) {
            Logger.e("Couldn't copy video", e);
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
