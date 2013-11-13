package co.touchlab.customcamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;
import co.touchlab.customcamera.util.CameraUtils;
import co.touchlab.customcamera.util.ShareUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraActivity extends Activity
{
    private Camera camera;
    private MediaRecorder mediaRecorder;

    Camera.Size videoSize;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);

    private CameraPreviewView cameraPreviewView;

    private File lastFile;
    private VideoView videoView;
    private int width;
    private Integer miniWidth;
    private Integer miniHeight;
    private int height;
    private Button captureButton;
    private Wala playWala;
    private int cameraProfileId;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initPreviewSizes();

        extractFileAttachment();

        videoView = (VideoView) findViewById(R.id.videoView);
        cameraPreviewView = (CameraPreviewView) findViewById(R.id.camera_preview);
        cameraPreviewView.setZOrderOnTop(true);

        captureButton = (Button) findViewById(R.id.camera_button);
        captureButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (isRecording.compareAndSet(false, true))
                {
                    startVideo();
                }
                else
                {
                    captureButton.setText("Start Reply");
                    isRecording.set(false);
                    stopVideo();
                    sendEmail();
                }
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        //        if (!cameraInitialized)
        {
            openAndConfigureCamera(true);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        releaseMediaRecorder();
        releaseCamera();
    }

    private void openAndConfigureCamera(boolean afterPaused)
    {
        try
        {
            int frontCameraId = CameraUtils.getFrontCameraId();
            camera = Camera.open(frontCameraId);
            cameraProfileId = CameraUtils.findBestCameraProfile(frontCameraId);

            Camera.Parameters params = camera.getParameters();
            videoSize = CameraUtils.getVideoSize(params);
            params.setPreviewSize(videoSize.width, videoSize.height);
            camera.setParameters(params);

            cameraPreviewView.initCamera(camera, afterPaused);
        }
        catch (Exception e)
        {
            Log.d("##################", "Error opening camera: " + e.getMessage());
            releaseCamera();
        }
    }

    private void extractFileAttachment()
    {
        playWala = ShareUtils.extractAttachment(getIntent(), this);
    }

    private void sendEmail()
    {
        ShareUtils.shareEmail(this, lastFile);
    }

    private void initPreviewSizes()
    {
        width = Math.round(getResources().getDimension(R.dimen.large_preview_width));
        height = Math.round(getResources().getDimension(R.dimen.large_preview_height));

        miniWidth = Math.round(getResources().getDimension(R.dimen.small_preview_width));
        miniHeight = Math.round(getResources().getDimension(R.dimen.small_preview_height));
    }

    private void startVideo()
    {
        camera.stopPreview();
        camera.unlock();
        if (lastFile != null)
        {
            ViewGroup.LayoutParams layoutParams = cameraPreviewView.getLayoutParams();
            layoutParams.width = miniWidth;
            layoutParams.height = miniHeight;
            cameraPreviewView.setLayoutParams(layoutParams);

            playLastVideo();
            captureButton.setText("Stop Reply");
        }
        else
        {
            captureButton.setText("Stop Message");
        }

        if (prepareMediaRecorder())
        {
            mediaRecorder.start();
            camera.startPreview();
        }
        else
        {
            releaseMediaRecorder();
        }
    }

    private void playLastVideo()
    {
        videoView.setVideoPath(lastFile.getPath());

//        videoView.setRotation(180);
        videoView.start();
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp)
            {
                playbackDone();
            }
        });
    }

    private void playbackDone()
    {
        ViewGroup.LayoutParams layoutParams = cameraPreviewView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        cameraPreviewView.setLayoutParams(layoutParams);
    }

    private boolean prepareMediaRecorder()
    {
        mediaRecorder = new MediaRecorder();

        // Step 1: Attach camera to media recorder
        mediaRecorder.setCamera(camera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setProfile(CamcorderProfile.get(cameraProfileId));

        // No limit. Check the space on disk!
        mediaRecorder.setMaxDuration(-1);
        mediaRecorder.setVideoFrameRate(15);
        //mediaRecorder.setVideoSize(1280, 720);
//        mediaRecorder.setVideoSize(videoSize.width, videoSize.height);

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int mrRotate = 0;

        if (display.getRotation() == Surface.ROTATION_0)
            mrRotate = 270;


        if (display.getRotation() == Surface.ROTATION_270)
            mrRotate = 180;

        if (mrRotate != 0)
            mediaRecorder.setOrientationHint(mrRotate);

        // Step 4: Set output file
        File file = new File(Environment.getExternalStorageDirectory(), "vid_" + System.currentTimeMillis() + ".mp4");
        lastFile = file;
        mediaRecorder.setOutputFile(file.getPath());

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(cameraPreviewView.getHolder().getSurface());

        try
        {
            mediaRecorder.prepare();
        }
        catch (IllegalStateException e)
        {
            Log.d("##################", "MediaRecorder prepared in wrong order: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        catch (IOException e)
        {
            Log.d("##################", "MediaRecorder prepare failed: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }

        return true;
    }

    private void stopVideo()
    {
        mediaRecorder.stop();
        releaseMediaRecorder();
        videoView.stopPlayback();

        //todo: pulled out a bunch of code here that handles saving a file with the video

        Toast.makeText(this, "Done video!", Toast.LENGTH_SHORT).show();
    }

    private void releaseCamera()
    {
        if (camera != null)
        {
            camera.release();
        }
    }

    private void releaseMediaRecorder()
    {
        if (mediaRecorder != null)
        {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            try
            {
                camera.reconnect();
            }
            catch (IOException e)
            {
                Log.d("##################", "Media Recorder - error when releasing camera: " + e.getMessage());
            }
        }
    }
}
