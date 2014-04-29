package com.chatwala.android.camera;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
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
public class ConversationStarterFragment extends ChatwalaFragment {
    private CWCamera camera;
    private ChatwalaRecordingTexture recordingSurface;
    private ImageView actionImage;
    private TextView bottomText;
    private CountDownTimer countdownTimer;

    public ConversationStarterFragment() {}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        actionImage = new ImageView(getActivity());
        actionImage.setImageResource(R.drawable.record_circle);
        getCwActivity().setActionView(actionImage);

        bottomText = generateCwTextView(R.string.basic_instructions, Color.TRANSPARENT);
        setBottomView(bottomText);
    }

    @Override
    public void onSurfaceCreated(ChatwalaRecordingTexture recordingSurface) {
        Logger.i("Surface is ready for ConversationStarterFragment");
        this.recordingSurface = recordingSurface;
        if(!isSurfaceAttached()) {
            addViewToTop(recordingSurface, false);
            setIsSurfaceAttached(true);
        }
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

    @Override
    public void onStop() {
        super.onStop();

        setIsSurfaceAttached(false);
    }
}
