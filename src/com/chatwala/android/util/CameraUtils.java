package com.chatwala.android.util;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Build;
import com.chatwala.android.R;

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
        if (sizes == null)
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
        if (supportedVideoSizes == null || supportedVideoSizes.size() == 0)
            return null;

        Camera.Size best = supportedVideoSizes.get(0);

        //Get biggest
        for (Camera.Size supportedVideoSize : supportedVideoSizes)
        {
            if(supportedVideoSize.width > best.width)
                best = supportedVideoSize;
        }

        for (int i = 1; i < supportedVideoSizes.size(); i++)
        {
            Camera.Size size = supportedVideoSizes.get(i);
            if (size.width >= containerHeight && size.width < best.width)
                best = size;
        }

        assert best.width <= containerHeight;

        Logger.i("Best width: " + best.width + "\n\tBest height: " + best.height);

        return best;
    }

    public static int findVideoFrameRate(Context context)
    {
        Logger.i("Build.MODEL:\n\t" + Build.MODEL +
                "\nBuild.DEVICE:\n\t" + Build.DEVICE +
                "\nBuild.PRODUCT:\n\t" + Build.PRODUCT);

        //S4 can't handle 24 fps
        if (DeviceUtils.isDeviceS4())
            return context.getResources().getInteger(R.integer.video_frame_rate_s4);
        else
            return context.getResources().getInteger(R.integer.video_frame_rate);
    }

    public static int findVideoBitDepth(Context context)
    {
        /*int minimumBitRate = context.getResources().getInteger(R.integer.video_bid_depth);
        float bitRateRatio = ((float)context.getResources().getInteger(R.integer.bit_rate_ratio))/1000;
        return Math.max(Math.round((float) width * (float) height * bitRateRatio), minimumBitRate);*/
        //S4 needs higher rate due to bigger resolution
        if (DeviceUtils.isDeviceS4())
            return context.getResources().getInteger(R.integer.video_bid_depth_s4);
        else
            return context.getResources().getInteger(R.integer.video_bid_depth);
    }

    public static int findVideoMinWidth(Context context)
    {
        //S4 needs higher rate due to bigger resolution
        if (DeviceUtils.isDeviceS4())
            return context.getResources().getInteger(R.integer.video_min_width_s4);
        else
            return context.getResources().getInteger(R.integer.video_min_width);
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

    public static void setRecordingHintIfNecessary(Camera.Parameters params)
    {
        if (DeviceUtils.isDeviceS4())
        {
//            params.setRecordingHint(true);
//            params.get
//            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
    }


    //TODO: This needs fixing. HTC One returns nonsense. S4 can't do default. Several other Samsung phones have the same issue.
    public static int findCameraFrameRate(Context context, Camera.Parameters params)
    {
        int defaultRate = context.getResources().getInteger(R.integer.video_frame_rate);

        int maxReturnedRate = 0;
        int setRate = Integer.MAX_VALUE;

        List<int[]> supportedPreviewFpsRange = params.getSupportedPreviewFpsRange();
        for (int[] rateRange : supportedPreviewFpsRange)
        {
            for (int rate1000 : rateRange)
            {
                int rate = (int) Math.round((double) rate1000 / 1000d);
                if (rate >= defaultRate && rate < setRate)
                    setRate = rate;

                if (rate > maxReturnedRate)
                    maxReturnedRate = rate;

            }

        }

        if(setRate > 30)
            return 30;

        if (setRate < Integer.MAX_VALUE)
            return setRate;

        if (maxReturnedRate > 0)
            return maxReturnedRate;

        throw new RuntimeException("Coudn't find compatible frame rate");
    }
}
