package co.touchlab.customcamera.util;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Build;
import co.touchlab.customcamera.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/9/13
 * Time: 6:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class CameraUtils
{
    public static File getRootDataFolder(Context context)
    {
        File filesDir = context.getFilesDir();
        File video_data = new File(filesDir, "video_data");
        video_data.mkdirs();
        return video_data;
    }
    public static int getFrontCameraId() throws Exception
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

    public static Camera.Size getVideoSize(int containerHeight, Camera.Parameters parameters)
    {
        //Create new array in case this is immutable
        List<Camera.Size> supportedVideoSizes = new ArrayList<Camera.Size>(parameters.getSupportedPreviewSizes());
        return findBestFitCameraSize(containerHeight, supportedVideoSizes);
    }

    public static Camera.Size findCameraVideoSize(int containerHeight, Camera.Parameters parameters)
    {
        //Create new array in case this is immutable
        List<Camera.Size> sizes = parameters.getSupportedVideoSizes();
        if(sizes == null)
            sizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedVideoSizes = new ArrayList<Camera.Size>(sizes);
        return findBestFitCameraSize(containerHeight, supportedVideoSizes);
    }

    public static Camera.Size findCameraPreviewSize(int containerHeight, Camera.Parameters parameters)
    {
        //Create new array in case this is immutable
        List<Camera.Size> supportedVideoSizes = new ArrayList<Camera.Size>(parameters.getSupportedPreviewSizes());
        return findBestFitCameraSize(containerHeight, supportedVideoSizes);
    }

    private static Camera.Size findBestFitCameraSize(int containerHeight, List<Camera.Size> supportedVideoSizes)
    {
        if(supportedVideoSizes == null || supportedVideoSizes.size() == 0)
            return null;

        Camera.Size best = supportedVideoSizes.get(0);

        for(int i=1; i<supportedVideoSizes.size(); i++)
        {
            Camera.Size size = supportedVideoSizes.get(i);
            if(size.width >= containerHeight && size.width < best.width)
                best = size;
        }

        assert best.width <= containerHeight;

        CWLog.i("BEST: width: " + best.width + "/height: " + best.height);

        return best;
    }

    public static int findVideoFrameRate(Context context)
    {
        CWLog.i("Build.MODEL: "+ Build.MODEL);
        CWLog.i("Build.DEVICE: "+ Build.DEVICE);
        CWLog.i("Build.PRODUCT: "+ Build.PRODUCT);

        //S4 can't handle 24 fps
        if(Build.DEVICE.startsWith("jflte"))
            return context.getResources().getInteger(R.integer.video_frame_rate_s4);
        else
            return context.getResources().getInteger(R.integer.video_frame_rate);
    }

    public static int findBestCameraProfile(int cameraId)
    {
        if (CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_CIF) != null)
            return CamcorderProfile.QUALITY_CIF;
        else if (CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QCIF) != null)
            return CamcorderProfile.QUALITY_QCIF;
        else if (CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P) != null)
            return CamcorderProfile.QUALITY_480P;
        else
            throw new RuntimeException("No compatible camera");
    }
}
