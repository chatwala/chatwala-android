package com.chatwala.android.util;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import com.chatwala.android.ChatwalaApplication;

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
    private static final int MIN_SPACE_MEGS = 5;
    private static final int MAX_SPACE_MEGS = 500;
    private static final int BYTES_IN_MEG = 1048576;
    private static final int MIN_INBOX_MESSAGES = 5;

    private static ChatwalaApplication chatwalaApplication = null;
    private static File tempDir, messageDir, outboxDir;

    private static final String WALA_FILE_PREFIX = "vid_";
    private static final String CHAT_DIR_PREFIX = "chat_";
    private static final String SHARE_DIR_PREFIX = "sharefile_";
    private static final String WALA_FILE_EXTENSION = ".wala";
    private static final String MP4_FILE_EXTENSION = ".mp4";

    private static final String PREPPED_WALA_FILE = "chat.wala";
    private static final String PREPPED_METADATA_FILE = "metadata.json";
    private static final String PREPPED_VIDEO_FILE = "video.mp4";

    public static boolean init(ChatwalaApplication application)
    {
        try
        {
            chatwalaApplication = application;
            initDataStore(chatwalaApplication);
            return isEnoughSpace();
        }
        catch (Exception e)
        {
            CWLog.softExceptionLog(MessageDataStore.class, "Problem initializing DataStore", e);
            return false;
        }
    }

    public static boolean isEnoughSpace()
    {
        long megAvailable = megsAvailable();
        CWLog.i(MessageDataStore.class, "Mb Available: " + megAvailable);
        Log.d("########", "Mb Available: " + megAvailable);
        return megAvailable > MIN_SPACE_MEGS;
    }

    /**
     * Mixed messages with exceptions and boolean.  Need to pick a lane.
     * @return boolean true if trimming was performed.
     * @throws IOException
     */
    public static boolean checkClearStore() throws IOException
    {
        long spaceUsed = megsUsed();
        long spaceLeft = MAX_SPACE_MEGS - spaceUsed;

        CWLog.i(MessageDataStore.class, "Mb Used: " + spaceUsed);
        Log.d("########", "Mb Used: " + spaceUsed);
        CWLog.i(MessageDataStore.class, "Mb Left: " + spaceLeft);
        Log.d("########", "Mb Left: " + spaceLeft);

        if(spaceLeft < MIN_SPACE_MEGS)
        {
            trimOld(spaceLeft);
            return true;
        }
        else
        {
            return false;
        }
    }

    public static void dumpMessageStore()
    {
        CWLog.i(MessageDataStore.class, "Dumping message store");
        Log.d("########", "Dumping message store");
        dumpStore(messageDir);
    }

    private static void dumpStore(File dirToDump)
    {
        File[] files = dirToDump.listFiles();
        for(File file : files)
        {
            file.delete();
        }
    }

    public static File findMessageInLocalStore(String id)
    {
        return new File(messageDir, MessageDataStore.WALA_FILE_PREFIX+ id +MessageDataStore.WALA_FILE_EXTENSION);
    }

    private static long megsUsed()
    {
        File videosDir = messageDir;

        long total = 0;
        File[] files = videosDir.listFiles();

        for (File file : files)
        {
            Log.d("#######", "File: " + file.getPath() + " Size: " + file.length());
            total += file.length();
        }

        return total / BYTES_IN_MEG;
    }

    private static void trimOld(long spaceLeft) throws IOException
    {
        File videoDir = messageDir;
        File[] files = videoDir.listFiles();
        Arrays.sort(files, new Comparator<File>()
        {
            @Override
            public int compare(File lhs, File rhs)
            {
                return (int)(rhs.lastModified() - lhs.lastModified());
            }
        });

        if(files.length < MIN_INBOX_MESSAGES)
        {
            throw new IOException("Store limit exeeded with less than 5 files.");
        }
        else
        {
            CWLog.i(MessageDataStore.class, "Deleting the oldest message");
            files[0].delete();
        }
    }

    private static long megsAvailable()
    {
        StatFs stat = new StatFs(messageDir.getPath());
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

    public static void initDataStore(Application application)
    {
        tempDir = new File(application.getFilesDir(), "temp");
        messageDir = new File(application.getFilesDir(), "messages");
        outboxDir = new File(application.getFilesDir(), "outbox");

        tempDir.mkdir();
        messageDir.mkdir();
        outboxDir.mkdir();
    }

    public static File makeTempWalaFile()
    {
        return makeWalaFile(tempDir);
    }

    public static File makeMessageWalaFile()
    {
        return makeWalaFile(messageDir);
    }

    public static File makeTempVideoFile()
    {
        return new File(tempDir, WALA_FILE_PREFIX + System.currentTimeMillis() + MP4_FILE_EXTENSION);
    }

    public static File makeTempChatDir()
    {
        return makeChatDir(tempDir);
    }

    public static File makeMessageChatDir()
    {
        return makeChatDir(messageDir);
    }

    public static File makeOutboxWalaFile()
    {
        File shareDir = new File(outboxDir , SHARE_DIR_PREFIX + System.currentTimeMillis());
        shareDir.mkdir();
        return new File(shareDir, PREPPED_WALA_FILE);
    }

    public static File makeMetadataFile(File targetDirectory)
    {
        return new File(targetDirectory, PREPPED_METADATA_FILE);
    }

    public static File makeVideoFile(File targetDirectory)
    {
        return new File(targetDirectory, PREPPED_VIDEO_FILE);
    }

//    private static File getTempDirectory(Application application)
//    {
//        File dir = new File(application.getFilesDir(), "temp");
//        dir.mkdir();
//        return dir;
//    }
//
//    private static File getMessageStorageDirectory(Application application)
//    {
//        File dir = new File(application.getFilesDir(), "messages");
//        dir.mkdir();
//        return dir;
//    }
//
//    private static File getOutboxDirectory(Application application)
//    {
//        File dir = new File(application.getFilesDir(), "outbox");
//        dir.mkdir();
//        return dir;
//    }

    private static File makeWalaFile(File targetDirectory)
    {
        return new File(targetDirectory, MessageDataStore.WALA_FILE_PREFIX + System.currentTimeMillis() + MessageDataStore.WALA_FILE_EXTENSION);
    }

    private static File makeChatDir(File targetDirectory)
    {
        return new File(targetDirectory, MessageDataStore.CHAT_DIR_PREFIX + System.currentTimeMillis());
    }
}
