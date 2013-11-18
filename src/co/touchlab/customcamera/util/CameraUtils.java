package co.touchlab.customcamera.util;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import co.touchlab.customcamera.R;

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

    public static Camera.Size getVideoSize(Context context, Camera.Parameters parameters)
    {
        //Create new array in case this is immutable
        List<Camera.Size> supportedVideoSizes = new ArrayList<Camera.Size>(parameters.getSupportedPreviewSizes());
        return findBestFitCameraSize(context, supportedVideoSizes);
    }

    public static Camera.Size findCameraVideoSize(Context context, Camera.Parameters parameters)
    {
        //Create new array in case this is immutable
        List<Camera.Size> supportedVideoSizes = new ArrayList<Camera.Size>(parameters.getSupportedVideoSizes());
        return findBestFitCameraSize(context, supportedVideoSizes);
    }

    private static Camera.Size findBestFitCameraSize(Context context, List<Camera.Size> supportedVideoSizes)
    {
        int minWidth = context.getResources().getInteger(R.integer.video_min_width);
        Camera.Size best = supportedVideoSizes.get(0);

        for(int i=1; i<supportedVideoSizes.size(); i++)
        {
            Camera.Size size = supportedVideoSizes.get(i);
            if(size.width >= minWidth && size.width < best.width)
                best = size;
        }

        assert best.width >= minWidth;

        CWLog.i("width: " + best.width + "/height: " + best.height);

        return best;
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
