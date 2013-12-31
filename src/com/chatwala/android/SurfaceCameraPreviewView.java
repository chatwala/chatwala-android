package com.chatwala.android;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import com.chatwala.android.util.CameraUtils;
import com.chatwala.android.util.MessageDataStore;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/24/13
 * Time: 4:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class SurfaceCameraPreviewView extends SurfaceView implements SurfaceHolder.Callback
{
    Camera camera = null;
    MediaRecorder mediaRecorder = null;

    private Camera.Size cameraVideoSize = null;
    private File recordingFile;
    private final CameraPreviewCallback callback;

    public SurfaceCameraPreviewView(Context context, CameraPreviewCallback callback)
    {
        super(context);
        initSurface();
        openCamera();
        this.callback = callback;
        //setCameraParams();
    }

    public SurfaceCameraPreviewView(Context context, AttributeSet attrs, CameraPreviewCallback callback)
    {
        this(context, callback);
    }

    public SurfaceCameraPreviewView(Context context, AttributeSet attrs, int defStyle, CameraPreviewCallback callback)
    {
        this(context, callback);
    }

    public interface CameraPreviewCallback
    {
        void surfaceReady();
        void recordingDone(File videoFile);
    }

    public void initSurface()
    {
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void openCamera()
    {
        try
        {
            camera = Camera.open(CameraUtils.getFrontCameraId());
        }
        catch (Exception e)
        {
            Log.d("##################", "Error opening camera: " + e.getMessage());
            camera.release();
        }
    }

    private void setCameraParams()
    {
        Camera.Parameters params = camera.getParameters();

        //These all kind of suck because they cause crashes, but we may need to use them to smooth out the recording process
        //params.setRecordingHint(true);
        //params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        //params.setPreviewFpsRange(15, 15);

        camera.setParameters(params);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if(cameraVideoSize != null)
        {
            try
            {
                camera.setPreviewDisplay(getHolder());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            camera.startPreview();

            prepareMediaRecorder();

            //http://stackoverflow.com/questions/3841122/android-camera-preview-is-sideways/5110406#5110406
            Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

            if (display.getRotation() == Surface.ROTATION_0)
            {
                camera.setDisplayOrientation(90);
            }

            if (display.getRotation() == Surface.ROTATION_270)
            {
                camera.setDisplayOrientation(180);
            }

            callback.surfaceReady();
        }
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

        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int mrRotate = 0;

        if (display.getRotation() == Surface.ROTATION_0)
            mrRotate = 270;


        if (display.getRotation() == Surface.ROTATION_270)
            mrRotate = 180;

        if (mrRotate != 0)
            mediaRecorder.setOrientationHint(mrRotate);

        // Step 4: Set output file
        recordingFile = new File(MessageDataStore.getTempDirectory(((Activity)getContext()).getApplication()), "vid_" + System.currentTimeMillis() + ".mp4");
        mediaRecorder.setOutputFile(recordingFile.getPath());

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(getHolder().getSurface());

        try
        {
            mediaRecorder.prepare();
        }
        catch (IllegalStateException e)
        {
            releaseMediaRecorder();
            return false;
        }
        catch (IOException e)
        {
            releaseMediaRecorder();
            return false;
        }

        return true;
    }

    public void startRecording()
    {
        camera.unlock();
        mediaRecorder.start();
    }

    public void stopRecording()
    {
        mediaRecorder.stop();
        callback.recordingDone(recordingFile);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if(((ViewGroup)getParent()).getHeight() != 0)
        {
            setCameraPreviewSize();

            //either scale up the height of the preview, so that is at least the size of the container
            int viewHeight = ((ViewGroup)getParent()).getHeight();
            int previewHeight = cameraVideoSize.width;
            int previewWidth = cameraVideoSize.height;
            double ratio = (double) viewHeight / (double) previewWidth;

            double newPreviewHeight = (double) previewHeight * ratio;
            double newPreviewWidth = (double) previewWidth * ratio;

            //Preview is rotated 90 degrees, so swap width/height
            setMeasuredDimension((int)newPreviewWidth, (int)newPreviewHeight);
        }
        else
        {
            //The surface needs a non-zero size for the callbacks to trigger
            setMeasuredDimension(1, 1);
        }
    }

    private void setCameraPreviewSize()
    {
        Camera.Parameters params = camera.getParameters();

        cameraVideoSize = CameraUtils.findCameraVideoSize(((ViewGroup)getParent()).getHeight(), params);
        params.setPreviewSize(cameraVideoSize.width, cameraVideoSize.height);

        camera.setParameters(params);
    }

    public void releaseResources()
    {
        releaseCamera();
        releaseMediaRecorder();
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