package com.chatwala.android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.text.DecimalFormat;

public class NetworkUtils {
    /**
     * Returns true if the device is connected to a network.
     * Note that this does not mean there is an internet connection.
     *
     * @param context A Context used to access the ConnectivityManager.
     *
     * @return true if the device is connected to a network, otherwise false.
     */
    public static boolean isConnectedToNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    /**
     * Returns true if the device is connected to the internet.
     * TODO implement a ping to a Chatwala server to determine if we can actually connect.
     *
     * @param context A Context used to access the ConnectivityManager.
     *
     * @return true if the device is connected to the internet, otherwise false.
     */
    public static boolean isConnectedToInternet(Context context) {
        return isConnectedToNetwork(context);
    }

    /**
     * Returns a <code>String</code> formatting the amount of bytes passed in, so that they are displayed in the
     * highest denomination of memory that keeps the value to greater than 0.
     *
     * @param bytes the amount of bytes (as a long) we want to get a pretty <code>String</code> for.
     *
     * @return a pretty <code>String</code> representing the amount of bytes passed in.
     */
    public static String getPrettyByteCount(long bytes) {
        return getPrettyByteCount((double) bytes);
    }

    /**
     * Returns a <code>String</code> formatting the amount of bytes passed in, so that they are displayed in the
     * highest denomination of memory that keeps the value to greater than 0.
     *
     * @param bytes the amount of bytes (as a double) we want to get a pretty <code>String</code> for.
     *
     * @return a pretty <code>String</code> representing the amount of bytes passed in.
     */
    public static String getPrettyByteCount(double bytes) {
        if(bytes < 1) {
            return "0 B";
        }
        DecimalFormat form = new DecimalFormat("0.00");
        if(bytes / 1024 < 1) {
            return form.format(bytes) + " B";
        }
        bytes /= 1024;
        if(bytes / 1204 < 1) {
            return form.format(bytes) + " kb";
        }
        bytes /= 1024;
        if(bytes / 1024 < 1) {
            return form.format(bytes) + " mb";
        }
        bytes /= 1024;
        if(bytes / 1024 < 1) {
            return form.format(bytes) + " gb";
        }
        bytes /= 1024;
        if(bytes / 1024 < 1) {
            return form.format(bytes) + " tb";
        }
        bytes /= 1024;
        return form.format(bytes) + " pb";
    }
}
