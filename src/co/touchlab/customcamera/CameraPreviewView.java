package co.touchlab.customcamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
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
    Camera camera = null;

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

    public void initSurface(Camera camera)
    {
        this.camera = camera;
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
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
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
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
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {

    }
}
