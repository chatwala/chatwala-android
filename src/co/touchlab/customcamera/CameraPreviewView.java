package co.touchlab.customcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import co.touchlab.customcamera.util.CameraUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 4/22/13
 * Time: 9:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class CameraPreviewView extends TextureView implements TextureView.SurfaceTextureListener
{
    Camera camera = null;
    MediaRecorder mediaRecorder = null;

    private Camera.Size cameraPreviewSize = null;
    private Camera.Size cameraVideoSize = null;
    private File recordingFile;
    private final CameraPreviewCallback callback;
    private AtomicBoolean recordStarting = new AtomicBoolean(false);

    public interface CameraPreviewCallback
    {
        void surfaceReady();
        void recordingStarted();
        void recordingDone(File videoFile);
    }

    public CameraPreviewView(Context context, CameraPreviewCallback callback)
    {
        super(context);
        initSurface();
        openCamera();
        this.callback = callback;
    }

    public CameraPreviewView(Context context, AttributeSet attrs, CameraPreviewCallback callback)
    {
        this(context, callback);
    }

    public CameraPreviewView(Context context, AttributeSet attrs, int defStyle, CameraPreviewCallback callback)
    {
        this(context, callback);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        Log.w(CameraPreviewView.class.getSimpleName(), "onSurfaceTextureAvailable: "+ surface);
        if(cameraPreviewSize != null)
        {
            setCameraParams();
            try
            {
                camera.setPreviewTexture(surface);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            camera.startPreview();

            prepareMediaRecorder();

            //http://stackoverflow.com/questions/3841122/android-camera-preview-is-sideways/5110406#5110406
            Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

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

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
        
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        Log.w(CameraPreviewView.class.getSimpleName(), "onSurfaceTextureDestroyed");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        Log.w(CameraPreviewView.class.getSimpleName(), "onSurfaceTextureUpdated: " + surface);
//        Log.i(CameraPreviewCallback.class.getSimpleName(), "updated: " + System.currentTimeMillis());
        if(recordStarting.getAndSet(false))
        {
            callback.recordingStarted();
        }
    }

    public void initSurface()
    {
//        SurfaceHolder surfaceHolder = getHolder();
//        surfaceHolder.addCallback(this);
//        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setSurfaceTextureListener(this);
    }

    private void openCamera()
    {
        try
        {
            camera = Camera.open(CameraUtils.getFrontCameraId());
            camera.setErrorCallback(new Camera.ErrorCallback()
            {
                @Override
                public void onError(int error, Camera camera)
                {
                    Log.w(CameraPreviewView.class.getSimpleName(), "onError: "+ error +"/Camera: "+ camera);
                }
            });
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
        CameraUtils.setRecordingHintIfNecessary(params);
        //params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        //params.setPreviewFpsRange(15, 15);

        camera.setParameters(params);
    }

    /*@Override
    public void surfaceCreated(SurfaceHolder holder)
    {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {

    }*/

    private boolean prepareMediaRecorder()
    {
        mediaRecorder = new MediaRecorder();

        // Step 1: Attach camera to media recorder
        mediaRecorder.setCamera(camera);

        // Step 2: Set sources

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoFrameRate(CameraUtils.findVideoFrameRate(getContext()));

        mediaRecorder.setVideoSize(cameraVideoSize.width, cameraVideoSize.height);

        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        mediaRecorder.setVideoEncodingBitRate(CameraUtils.findVideoBitDepth(getContext()));

        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int mrRotate = 0;

        if (display.getRotation() == Surface.ROTATION_0)
            mrRotate = 270;


        if (display.getRotation() == Surface.ROTATION_270)
            mrRotate = 180;

        if (mrRotate != 0)
            mediaRecorder.setOrientationHint(mrRotate);

        // Step 4: Set output file
        recordingFile = new File(CameraUtils.getRootDataFolder(getContext()), "vid_" + System.currentTimeMillis() + ".mp4");
        mediaRecorder.setOutputFile(recordingFile.getPath());

        // Step 5: Set the preview output
//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
//            mediaRecorder.setPreviewDisplay(new Surface(this.getSurfaceTexture()));
//        mediaRecorder.setPreviewDisplay(getHolder().getSurface());

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
        Log.i(CameraPreviewCallback.class.getSimpleName(), "startRecording: " + System.currentTimeMillis());
        mediaRecorder.start();
        recordStarting.set(true);
    }

    public void stopRecording()
    {
        mediaRecorder.stop();
        callback.recordingDone(recordingFile);
    }

    public void abortRecording()
    {
        mediaRecorder.stop();
    }

    /*@Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {

    }*/

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (((ViewGroup) getParent()).getHeight() != 0)
        {
            findCameraSizes();

            //either scale up the height of the preview, so that is at least the size of the container
            int viewWidth = ((ViewGroup) getParent()).getWidth();
            int previewHeight = cameraPreviewSize.width;
            int previewWidth = cameraPreviewSize.height;
            double ratio = (double) viewWidth / (double) previewWidth;

            double newPreviewHeight = (double) previewHeight * ratio;
            double newPreviewWidth = (double) previewWidth * ratio;

            //Preview is rotated 90 degrees, so swap width/height
            setMeasuredDimension((int) newPreviewWidth, (int) newPreviewHeight);
        }
        else
        {
            //The surface needs a non-zero size for the callbacks to trigger
            setMeasuredDimension(1, 1);
        }
    }

    private void findCameraSizes()
    {
        Camera.Parameters params = camera.getParameters();

        int viewWidth = CameraUtils.findVideoMinWidth(getContext());
        cameraPreviewSize = CameraUtils.findCameraPreviewSize(viewWidth, params);
        cameraVideoSize = CameraUtils.findCameraVideoSize(viewWidth, params);
        params.setPreviewSize(cameraPreviewSize.width, cameraPreviewSize.height);

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
