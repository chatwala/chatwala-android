package co.touchlab.customcamera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraActivity extends Activity
{
    private Camera camera;
    private MediaRecorder mediaRecorder;

    Camera.Size videoSize;
    private boolean cameraInitialized;
    private AtomicBoolean isRecording;

    private CameraPreviewView cameraPreviewView;

    private File lastFile;
    private VideoView videoView;
    private MediaController mc;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        cameraPreviewView = (CameraPreviewView) findViewById(R.id.camera_preview);
        cameraPreviewView.setZOrderOnTop(true);

        openAndConfigureCamera(false);
        isRecording = new AtomicBoolean(false);

        ImageView captureButton = (ImageView) findViewById(R.id.camera_button);
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
                    isRecording.set(false);
                    stopVideo();
                }
            }
        });
    }

    private void openAndConfigureCamera(boolean afterPaused)
    {
        try
        {
            camera = Camera.open(getFrontCameraId());

            Camera.Parameters params = camera.getParameters();
            videoSize = getVideoSize(params);
            params.setPreviewSize(videoSize.width, videoSize.height);
            camera.setParameters(params);

            cameraPreviewView.initCamera(camera, afterPaused);

            cameraInitialized = true;
        }
        catch (Exception e)
        {
            Log.d("##################", "Error opening camera: " + e.getMessage());
            releaseCamera();
        }
    }

    private int getFrontCameraId() throws Exception
    {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++)
        {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
            {
                return i;
            }
        }

        return 0;
    }

    private static Camera.Size getVideoSize(Camera.Parameters params)
    {
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        Camera.Size videoSize = previewSizes.get(0);
        return videoSize;
    }

    private void startVideo()
    {
        camera.stopPreview();
        camera.unlock();
        if (lastFile != null)
        {
            playLastVideo();
        }

        if (prepareMediaRecorder())
        {
            toggleRecordSettings();

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
        videoView = (VideoView) findViewById(R.id.videoView);
        mc = new MediaController(this);
        mc.setAnchorView(videoView);
        mc.setMediaPlayer(videoView);
        videoView.setMediaController(mc);
//        videoView.setZOrderMediaOverlay(false);
        videoView.setVideoPath(lastFile.getPath());

//        videoView.setRotation(180);
        videoView.start();
    }

    private boolean prepareMediaRecorder()
    {
        mediaRecorder = new MediaRecorder();

        // Step 1: Attach camera to media recorder
        mediaRecorder.setCamera(camera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        int profile;

//        if (CamcorderProfile.get(0, CamcorderProfile.QUALITY_CIF) != null)
            profile = CamcorderProfile.QUALITY_CIF;
//        else if (CamcorderProfile.get(0, CamcorderProfile.QUALITY_QCIF) != null)
//            profile = CamcorderProfile.QUALITY_QCIF;
//        else// if (CamcorderProfile.get(0, CamcorderProfile.QUALITY_480P) != null)
//            profile = CamcorderProfile.QUALITY_LOW;
//        else
//            throw new RuntimeException("No compatible camera");

        mediaRecorder.setProfile(CamcorderProfile.get(profile));


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
//        cameraPreviewView.setZOrderMediaOverlay(true);

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
//        videoView.stopPlayback();

        //todo: pulled out a bunch of code here that handles saving a file with the video

        toggleRecordSettings();
        Toast.makeText(this, "Done video!", Toast.LENGTH_SHORT).show();
    }

    private void toggleRecordSettings()
    {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (!isRecording.get())
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (!cameraInitialized)
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

    private void releaseCamera()
    {
        if (camera != null)
        {
            camera.release();
        }
        cameraInitialized = false;
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
