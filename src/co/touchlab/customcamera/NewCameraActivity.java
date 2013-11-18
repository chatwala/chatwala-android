package co.touchlab.customcamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import co.touchlab.customcamera.util.CameraUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 11/18/13
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewCameraActivity extends Activity
{
    Camera camera = null;
    MediaRecorder mediaRecorder = null;

    CameraPreviewView cameraPreviewView;
    private Camera.Size previewVideoSize;
    private Camera.Size cameraVideoSize;

    private Button mainActionButton;
    private AtomicBoolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        cameraPreviewView = (CameraPreviewView)findViewById(R.id.camera_preview);

        isRecording = new AtomicBoolean(false);

        mainActionButton = (Button) findViewById(R.id.camera_button);
        mainActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(isRecording.get())
                {
                    isRecording.set(false);
                    mediaRecorder.stop();
                }
                else
                {
                    isRecording.set(true);
                    camera.unlock();
                    mediaRecorder.start();
                }
            }
        });

        initMediaRecorder();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        releaseMediaRecorder();
        releaseCamera();
    }


    private void initMediaRecorder()
    {
        //http://developer.android.com/guide/topics/media/camera.html

        openAndConfigureCamera();
        connectAndStartCameraPreview();
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                prepareMediaRecorder();
            }
        }, 100);
    }

    private void openAndConfigureCamera()
    {
        try
        {
            camera = Camera.open(CameraUtils.getFrontCameraId());

            Camera.Parameters params = camera.getParameters();
            previewVideoSize = CameraUtils.getVideoSize(NewCameraActivity.this, params);
            cameraVideoSize = CameraUtils.findCameraVideoSize(NewCameraActivity.this, params);
            params.setPreviewSize(previewVideoSize.width, previewVideoSize.height);
            camera.setParameters(params);
        }
        catch (Exception e)
        {
            Log.d("##################", "Error opening camera: " + e.getMessage());
            camera.release();
        }
    }

    private void connectAndStartCameraPreview()
    {
        cameraPreviewView.initSurface(camera);
    }

    private boolean prepareMediaRecorder()
    {
        mediaRecorder = new MediaRecorder();

        // Step 1: Attach camera to media recorder
        mediaRecorder.setCamera(camera);

        // Step 2: Set sources

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoFrameRate(getResources().getInteger(R.integer.video_frame_rate));


        mediaRecorder.setVideoSize(cameraVideoSize.width, cameraVideoSize.height);

        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        mediaRecorder.setVideoEncodingBitRate(getResources().getInteger(R.integer.video_bid_depth));

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
        }
    }
}
