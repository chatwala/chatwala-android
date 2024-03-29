package com.chatwala.android.util;

import android.os.Build;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/13/13
 * Time: 12:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class DeviceUtils
{
    private static final String BUILD_DEVICE_S4 = "jflte";

    private static final String BUILD_MODEL_HTCONE = "HTC One";
    private static final String BUILD_DEVICE_HTCONE = "m7";

    public static boolean isDeviceS4()
    {
        return Build.DEVICE.startsWith(DeviceUtils.BUILD_DEVICE_S4);
    }

    public static boolean isDeviceHTCONE()
    {
        return Build.MODEL.startsWith(DeviceUtils.BUILD_MODEL_HTCONE);
    }
}
