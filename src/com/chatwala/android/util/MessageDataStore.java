package com.chatwala.android.util;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/24/13
 * Time: 5:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageDataStore
{
    public static final int MIN_SPACE_MEGS = 5;
    public static final int MAX_SPACE_MEGS = 500;
    public static final int BYTES_IN_MEG = 1048576;

    public static boolean isEnoughSpace(Application application)
    {
//        File file = messageStorageDirectory(application);
        long megAvailable = megsAvailable(application);

        return megAvailable > MIN_SPACE_MEGS;
    }

    /**
     * Mixed messages with exceptions and boolean.  Need to pick a lane.
     * @param application
     * @return
     * @throws IOException
     */
    public static boolean checkClearStore(Application application) throws IOException
    {
        long spaceLeft = megsAvailable(application);
        long spaceUsed = megsUsed(application);

        if(spaceLeft < MAX_SPACE_MEGS)
            throw new IOException("Not enough space");

        if(spaceUsed > MAX_SPACE_MEGS)
            trimOld(application);

        return true;
    }

    public static File findMessageInLocalStore(Application application, String id)
    {
        File dir = getMessageStorageDirectory(application);
        return new File(dir, "vid_"+ id +".wala");
    }

    private static long megsUsed(Application application)
    {
        File videosDir = getMessageStorageDirectory(application);

        long total = 0;
        File[] files = videosDir.listFiles();

        for (File file : files)
        {
            total += file.getTotalSpace();
        }

        return total / BYTES_IN_MEG;
    }

    private static void trimOld(Application application) throws IOException
    {
        File videoDir = getMessageStorageDirectory(application);
        File[] files = videoDir.listFiles();
        Arrays.sort(files, new Comparator<File>()
        {
            @Override
            public int compare(File lhs, File rhs)
            {
                return (int)(rhs.lastModified() - lhs.lastModified());
            }
        });

        if(files.length < 5)
            throw new IOException("Message store needs to be bigger");
        else
        {
            files[0].delete();
            files[1].delete();
            files[2].delete();
        }
    }

    private static long megsAvailable(Application application)
    {
        StatFs stat = new StatFs(getMessageStorageDirectory(application).getPath());
        long megAvailable;
        if (Build.VERSION.SDK_INT > 18)
        {
            long bytesAvailable = stat.getBlockSizeLong() *(long)stat.getBlockCountLong();
            megAvailable = bytesAvailable / BYTES_IN_MEG;
        }
        else
        {
            long bytesAvailable = (long)stat.getBlockSize() *(long)stat.getBlockCount();
            megAvailable = bytesAvailable / BYTES_IN_MEG;
        }
        return megAvailable;
    }

    public static File getTempDirectory(Application application)
    {
        File dir = new File(application.getFilesDir(), "temp");
        dir.mkdir();
        return dir;
    }

    public static File getMessageStorageDirectory(Application application)
    {
        File dir = new File(application.getFilesDir(), "messages");
        dir.mkdir();
        return dir;
    }

    public static File getOutboxDirectory(Application application)
    {
        File dir = new File(application.getFilesDir(), "outbox");
        dir.mkdir();
        return dir;
    }
}
