package com.chatwala.android.util;

/**
 * Created by matthewdavis on 1/24/14.
 */
public class TimestampUtils
{
    public static String formatMessageTimestamp(long messageTimestamp)
    {
        int secondsSince = (int)(System.currentTimeMillis() - messageTimestamp) / 1000;

        if(secondsSince < 60)
        {
            return secondsSince + "s";
        }
        else if(secondsSince < (60*60))
        {
            return (secondsSince/60) + "m";
        }
        else if(secondsSince < (60*60*60))
        {
            return ((secondsSince/60)/60) + "h";
        }
        else if(secondsSince < (60*60*60*24))
        {
            return (((secondsSince/60)/60)/24) + "d";
        }
        else
        {
            return ((((secondsSince/60)/60)/24)/7) + "w";
        }
    }
}
