package co.touchlab.customcamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 4/22/13
 * Time: 9:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class CameraPreviewView extends SurfaceView implements SurfaceHolder.Callback
{
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private boolean isPreviewRunning = false;

    public CameraPreviewView(Context context)
    {
        super(context);
    }

    public CameraPreviewView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public CameraPreviewView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }


    public void initCamera(Camera camera, boolean afterPaused)
    {
        this.camera = camera;
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if(afterPaused)
        {
            rotatePreview();
        }
    }

    private void startPreview()
    {
    	if(!isPreviewRunning)
    	{
	        try
	        {
	            camera.setPreviewDisplay(surfaceHolder);
	            camera.startPreview();
	            isPreviewRunning = true;
	        }
	        catch (IOException e)
	        {
	            Log.d("##################", "Error starting camera preview: " + e.getMessage());
	        }
    	}
    }
    
    private void stopPreview()
    {
    	if(isPreviewRunning)
    	{
	    	camera.stopPreview();
	    	isPreviewRunning = false;
    	}
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
//        stopPreview();
        rotatePreview();
//        startPreview();
    }


    private void rotatePreview()
    {
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
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {

    }
}
