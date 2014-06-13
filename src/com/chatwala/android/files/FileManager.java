package com.chatwala.android.files;

import android.os.Environment;
import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.ChatwalaSentMessage;
import com.chatwala.android.util.FileUtils;
import com.chatwala.android.util.Logger;

import java.io.File;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 2:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class FileManager {
    private static final int NUM_THREADS = 3;

    private static final String RECORDING_PREFIX = "recording_";
    private static final String RECORDING_FILE_EXTENSION = ".mp4";
    private static final String WALA_PREFIX = "wala_";
    private static final String WALA_FILE_EXTENSION = ".wala";
    private static final String WALA_VIDEO_FILE = "video.mp4";
    private static final String WALA_METADATA_FILE = "metadata.json";
    private static final String WALA_FILE = "chat.wala";

    private static final String PROFILE_PIC_EXTENSION = ".png";
    private static final String USER_IMAGE_EXTENSION = ".png";
    private static final String USER_THUMB_EXTENSION = ".thumb.png";
    private static final String USER_IMAGE_LAST_MODIFIED_EXTENSION = ".lastmodified";
    private static final String MESSAGE_IMAGE_EXTENSION = ".png";
    private static final String MESSAGE_THUMB_EXTENSION = ".thumb.png";
    private static final String USER_PROFILE_PIC_FILE = "user_profile_pic" + PROFILE_PIC_EXTENSION;

    private ChatwalaApplication app;
    private final ExecutorService queue;

    private ExecutorService getQueue() {
        return queue;
    }

    private File tmpDir;
    private File tmpImageDir;
    private File inboxDir;
    private File outboxDir;
    private File sentDir;
    private File messageThumbsDir;
    private File sentMessageThumbsDir;
    private File userThumbsDir;
    private File userDir;

    private ChatwalaApplication getApp() {
        return app;
    }

    private FileManager() {
        queue = Executors.newFixedThreadPool(NUM_THREADS);
    }

    private static class Singleton {
        public static final FileManager instance = new FileManager();
    }

    public static FileManager attachToApp(ChatwalaApplication app) {
        me().app = app;
        me().getQueue().execute(new Runnable() {
            @Override
            public void run() {
                me().loadFileStructure();
            }
        });
        return Singleton.instance;
    }

    public static FileManager getInstance() {
        return Singleton.instance;
    }

    private static FileManager me() {
        return Singleton.instance;
    }

    private void loadFileStructure() {
        try {
            tmpDir = getTopLevelDir("tmp");
            tmpImageDir = getTopLevelDir("tmp_img");
            inboxDir = getTopLevelDir("inbox");
            outboxDir = getTopLevelDir("outbox");
            sentDir = getTopLevelDir("sent");
            messageThumbsDir = getTopLevelDir("message_thumbs");
            sentMessageThumbsDir = getTopLevelDir("sent_message_thumbs");
            userThumbsDir = getTopLevelDir("user_thumbs");
            userDir = getTopLevelDir("user");

            FileUtils.deleteContents(tmpDir);
        }
        catch(Exception e) {
            Logger.e("Couldn't laod the file structure", e);
        }
    }

    private File getTopLevelDir(String name) {
        File dir = new File(getApp().getFilesDir(), name);
        dir.mkdir();
        return dir;
    }

    public static File getNewRecordingFile() {
        return new File(me().tmpDir, RECORDING_PREFIX + System.currentTimeMillis() + RECORDING_FILE_EXTENSION);
    }

    public static File getTempImageFile() {
        return new File(me().tmpImageDir, UUID.randomUUID().toString() + System.currentTimeMillis());
    }

    public static File getInboxDirForMessage(ChatwalaMessage message) {
        File f = new File(me().inboxDir, message.getMessageId());
        f.mkdirs();
        return f;
    }

    public static void clearMessageStorage() {
        me().getQueue().execute(new Runnable() {
            @Override
            public void run() {
                FileUtils.deleteContents(me().inboxDir);
                FileUtils.deleteContents(me().sentDir);
            }
        });
    }

    public static File getWalaFromInboxMessageDir(ChatwalaMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        return new File(getInboxDirForMessage(message), WALA_FILE);
    }

    public static File getVideoFromInboxMessageDir(ChatwalaMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        return new File(getInboxDirForMessage(message), WALA_VIDEO_FILE);
    }

    public static File getMetadataFromInboxMessageDir(ChatwalaMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        return new File(getInboxDirForMessage(message), WALA_METADATA_FILE);
    }

    public static File getOutboxDirForMessage(ChatwalaSentMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        File f = new File(me().outboxDir, message.getMessageId());
        f.mkdirs();
        return f;
    }

    public static File getVideoFromOutboxMessageDir(ChatwalaSentMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        return new File(getOutboxDirForMessage(message), WALA_VIDEO_FILE);
    }

    public static File getMetadataFromOutboxMessageDir(ChatwalaSentMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        return new File(getOutboxDirForMessage(message), WALA_METADATA_FILE);
    }

    public static File getWalaFromOutboxMessageDir(ChatwalaSentMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        return new File(getOutboxDirForMessage(message), WALA_FILE);
    }

    public static File getSentDirForMessage(ChatwalaSentMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        File f = new File(me().sentDir, message.getMessageId());
        f.mkdirs();
        return f;
    }

    public static File getWalaFromSentMessageDir(ChatwalaSentMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        return new File(getSentDirForMessage(message), WALA_FILE);
    }

    public static File getVideoFromSentMessageDir(ChatwalaSentMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        return new File(getSentDirForMessage(message), WALA_VIDEO_FILE);
    }

    public static File getMetadataFromSentMessageDir(ChatwalaSentMessage message) {
        if(message.getMessageId() == null) {
            return null;
        }
        return new File(getSentDirForMessage(message), WALA_METADATA_FILE);
    }

    public static File getUserProfilePic() {
        return new File(me().userDir, USER_PROFILE_PIC_FILE);
    }

    public static File getUserImage(String userId) {
        return new File(me().userThumbsDir, userId + USER_IMAGE_EXTENSION);
    }

    public static File getUserThumb(String userId) {
        return new File(me().userThumbsDir, userId + USER_THUMB_EXTENSION);
    }

    public static File getUserImageLastModified(String userId) {
        return new File(me().userThumbsDir, userId + USER_IMAGE_LAST_MODIFIED_EXTENSION);
    }

    public static File getMessageImage(ChatwalaMessage message) {
        return new File(me().messageThumbsDir, message.getMessageId() + MESSAGE_IMAGE_EXTENSION);
    }

    public static File getMessageThumb(ChatwalaMessage message) {
        return new File(me().messageThumbsDir, message.getMessageId() + MESSAGE_THUMB_EXTENSION);
    }

    public static File getSentMessageImage(ChatwalaSentMessage message) {
        return new File(me().sentMessageThumbsDir, message.getMessageId() + MESSAGE_IMAGE_EXTENSION);
    }

    public static File getSentMessageThumb(ChatwalaSentMessage message) {
        return new File(me().sentMessageThumbsDir, message.getMessageId() + MESSAGE_THUMB_EXTENSION);
    }

    private static String hash(String stringToHash) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] shad= md.digest(stringToHash.getBytes("UTF-8"));
        return byteArrayToHexString(shad);
    }

    private static String byteArrayToHexString(byte[] bytes) {
        String result = "";
        for(byte b : bytes) {
            result += Integer.toString((b & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static float getMessageStorageUsageInMb() {
        float usage = FileUtils.sizeOfDirectory(me().inboxDir) + FileUtils.sizeOfDirectory(me().sentDir);
        return usage / (1024 * 1024);
    }

    public static float getTotalStorageSpaceInMb() {
        float usage = FileUtils.getTotal(Environment.getDataDirectory());
        return usage / (1024 * 1024);
    }

    public static float getTotalAvailableSpaceInMb(){
        float available = FileUtils.getAvailable(Environment.getDataDirectory());
        return available / (1024 * 1024);
    }

    public static float getTotalStorageSpaceInGb() {
        float usage = FileUtils.getTotal(Environment.getDataDirectory());
        return usage / (1024 * 1024 * 1024);
    }

    public static float getTotalAvailableSpaceInGb(){
        float available = FileUtils.getAvailable(Environment.getDataDirectory());
        return available / (1024 * 1024 * 1024);
    }
}
