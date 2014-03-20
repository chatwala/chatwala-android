package com.chatwala.android.util;

/**
 * Created by matthewdavis on 1/24/14.
 */
public class TimestampUtils
{
    public static long HOUR_IN_MINUTES = 60;
    public static long DAY_IN_HOURS = 24;
    public static long WEEK_IN_DAYS = 7;

    public static long ONE_MINUTE = 60;
    public static long ONE_HOUR = ONE_MINUTE * HOUR_IN_MINUTES;
    public static long ONE_DAY = ONE_HOUR * DAY_IN_HOURS;
    public static long ONE_WEEK = ONE_DAY * WEEK_IN_DAYS;

    public static String formatMessageTimestamp(long messageTimestamp)
    {
        long secondsSince = (System.currentTimeMillis() - messageTimestamp) / 1000;

        if(secondsSince < ONE_MINUTE)
        {
            return secondsSince + "s";
        }
        else if(secondsSince < ONE_HOUR)
        {
            return (secondsSince/ONE_MINUTE) + "m";
        }
        else if(secondsSince < ONE_DAY)
        {
            return (secondsSince/ONE_HOUR) + "h";
        }
        else if(secondsSince < ONE_WEEK)
        {
            return (secondsSince/ONE_DAY) + "d";
        }
        else
        {
            return (secondsSince/ONE_WEEK) + "w";
        }
    }
}
