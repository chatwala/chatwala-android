package com.chatwala.android.util;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.ChatwalaApplication;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
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
    private static final int MIN_SPACE_MEGS = 10;
    private static final int BYTES_IN_MEG = 1048576;

    private static ChatwalaApplication chatwalaApplication = null;
    private static File tempDir, messageDir, outboxDir, usersDir, plistDir, thumbnailsDir;

    private static final String WALA_FILE_PREFIX = "vid_";
    private static final String CHAT_DIR_PREFIX = "chat_";
    private static final String SHARE_DIR_PREFIX = "sharefile_";
    private static final String THUMB_FILE_PREFIX = "thumb_";
    private static final String WALA_FILE_EXTENSION = ".wala";
    private static final String MP4_FILE_EXTENSION = ".mp4";
    private static final String PNG_FILE_EXTENSION = ".png";
    private static final String JPG_FILE_EXTENSION = ".jpg";

    private static final String PREPPED_WALA_FILE = "chat.wala";
    private static final String PREPPED_METADATA_FILE = "metadata.json";
    private static final String PREPPED_VIDEO_FILE = "video.mp4";

    private static final String PLIST_FILE = "killswitch.plist";



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
            Logger.e("Problem initializing DataStore", e);
            return false;
        }
    }

    public static boolean isEnoughSpace()
    {
        long megAvailable = megsAvailable();
        Logger.i("There are " + megAvailable + " Mb(s) available on the device");
        return megAvailable > MIN_SPACE_MEGS;
    }

    /**
     * Mixed messages with exceptions and boolean.  Need to pick a lane.
     * @return boolean true if trimming was performed.
     * @throws IOException
     */
    public static boolean checkClearStore(Context context)
    {
        long spaceUsed = megsUsed(messageDir);
        long spaceLeft = AppPrefs.getInstance(context).getPrefDiskSpaceMax() - spaceUsed;

        Logger.i(spaceUsed + " Mb(s) used on device");
        Logger.i(spaceLeft + " Mb(s) left on device");

//        Log.d("########", "Message Folder Size: " + getFileLengthRecursive(messageDir));
//        Log.d("########", "Outbox Folder Size: " + getFileLengthRecursive(outboxDir));
//        Log.d("########", "Temp Folder Size: " + getFileLengthRecursive(tempDir));
//        Log.d("########", "Image Folder Size: " + getFileLengthRecursive(usersDir));

        if(spaceLeft < 0)
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
        Logger.i("Dumping message store");
        deleteRecursive(messageDir);
        messageDir.mkdir();
    }

    public static void dumpTempStore()
    {
        Logger.i("Dumping temp store");
        deleteRecursive(tempDir);
        tempDir.mkdir();
    }

    private static void deleteRecursive(File fileOrDirectory)
    {
        if (fileOrDirectory.isDirectory())
        {
            for (File child : fileOrDirectory.listFiles())
            {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    private static long getFileLengthRecursive(File fileOrDirectory)
    {
        long total = 0;

        if(fileOrDirectory.isDirectory())
        {
            for(File child : fileOrDirectory.listFiles())
            {
                total += getFileLengthRecursive(child);
            }
        }

        total += fileOrDirectory.length();
        return total;
    }

    public static void logDirFileInfo(File directory)
    {
        StringBuilder allFiles = new StringBuilder();
        for(File file : directory.listFiles())
        {
            allFiles.append(file.getName() + " " + file.length() + " | ");
        }
        Logger.d("All files in directory " + directory.getAbsolutePath() + ":\n\t" + allFiles.toString());
    }

    public static File findMessageInLocalStore(String id)
    {
        return new File(messageDir, WALA_FILE_PREFIX + id + WALA_FILE_EXTENSION);
    }

    public static File findUserImageInLocalStore(String id)
    {
        return new File(usersDir, id + PNG_FILE_EXTENSION);
    }

    public static File findUserImageThumbInLocalStore(String id)
    {
        return new File(usersDir, THUMB_FILE_PREFIX + id + PNG_FILE_EXTENSION);
    }

    public static File findMessageThumbTempPathInLocalStore(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] shad= md.digest(url.getBytes("UTF-8"));
            String shadStr = byteArrayToHexString(shad);
            return new File(tempDir, shadStr + PNG_FILE_EXTENSION);
        }
        catch(Exception e) {
            Logger.e("Couldn't find the message thumb temp in for " + url);
        }
        return new File(tempDir, "error.png");

    }

    public static File findMessageThumbInLocalStore(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] shad= md.digest(url.getBytes("UTF-8"));
            String shadStr = byteArrayToHexString(shad);
            return new File(thumbnailsDir, shadStr + PNG_FILE_EXTENSION);
        }
        catch(Exception e) {
            Logger.e("Couldn't find the message thumb for " + url);
        }
        return new File(thumbnailsDir, "error.png");

    }

    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result +=
                    Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }

    private static long megsUsed(File dirToCheck)
    {
//        long total = 0;
//        File[] files = dirToCheck.listFiles();
//
//        for (File file : files)
//        {
//            total += file.length();
//        }

        return getFileLengthRecursive(dirToCheck) / BYTES_IN_MEG;
    }

    private static void trimOld(long spaceLeft) {
        File videoDir = messageDir;
        File[] files = videoDir.listFiles();
        Arrays.sort(files, new Comparator<File>()
        {
            @Override
            public int compare(File lhs, File rhs)
            {
                return Long.valueOf(lhs.lastModified()).compareTo(rhs.lastModified());
            }
        });

        long bytesUnderCap = spaceLeft * BYTES_IN_MEG;

        int i;
        for(i = 0; bytesUnderCap < 0; i++) {
            if(files[i].isDirectory()) {
                for(File file : files[i].listFiles()) {
                    bytesUnderCap += file.length();
                    file.delete();
                }
            }

            bytesUnderCap += files[i].length();
            files[i].delete();
        }
        Logger.i("Deleted the " + i + " oldest message");
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
        plistDir = new File(application.getFilesDir(), "plist");
        thumbnailsDir = new File(application.getFilesDir(), "thumbnails");


        tempDir.mkdir();
        messageDir.mkdir();
        outboxDir.mkdir();
        plistDir.mkdir();
        thumbnailsDir.mkdir();


        File imagesDir = new File(application.getFilesDir(), "images");
        imagesDir.mkdir();
        usersDir = new File(imagesDir, "profile");
        usersDir.mkdirs();

        dumpTempStore();
    }

    public static File makePlistFile()
    {
        return new File(plistDir, PLIST_FILE);
    }

    public static File makeTempUserFile(String userId)
    {
        return new File(tempDir, userId + JPG_FILE_EXTENSION);
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

    public static File makeOutboxVideoFile()
    {
        return new File(outboxDir, WALA_FILE_PREFIX + System.currentTimeMillis() + MP4_FILE_EXTENSION);
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
