package com.chatwala.android.files;

import android.content.res.Resources;
import android.graphics.Bitmap;
import com.chatwala.android.R;
import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.messages.ChatwalaMessageBase;
import com.chatwala.android.util.FileUtils;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.ThumbUtils;
import com.chatwala.android.util.VideoUtils;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 7:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageManager {
    private ChatwalaApplication app;

    private ChatwalaApplication getApp() {
        return app;
    }

    private ImageManager() {}

    private static class Singleton {
        public static final ImageManager instance = new ImageManager();
    }

    public static ImageManager attachToApp(ChatwalaApplication app) {
        me().app = app;
        return Singleton.instance;
    }

    public static ImageManager getInstance() {
        return Singleton.instance;
    }

    private static ImageManager me() {
        return Singleton.instance;
    }

    public static boolean profilePicExists() {
        return FileManager.getUserProfilePic().exists();
    }

    public static File createProfilePicFromVideoFrame(File video) {
        File profilePic = FileManager.getUserProfilePic();
        try {
            Bitmap frame = VideoUtils.createBitmapFromVideoFrame(video, 500);
            FileUtils.writeBitmapToFile(frame, profilePic);
            return profilePic;
        }
        catch(Throwable e) {
            Logger.e("Got an error while creating the user's profile pic from a video frame", e);
            return null;
        }
    }

    public static File createMessageThumbFromVideoFrame(File video, ChatwalaMessageBase message) {
        try {
            Bitmap frame = VideoUtils.createBitmapFromVideoFrame(video, 500);
            FileUtils.writeBitmapToFile(frame, message.getLocalMessageThumb());
            return message.getLocalMessageThumb();
        }
        catch(Throwable e) {
            Logger.e("Got an error while creating a message's thumb from a video frame", e);
            return null;
        }
    }

    public static File createDrawerThumbFromFile(File from, File to) {
        Resources res = me().getApp().getResources();
        float width = res.getDimension(R.dimen.thumb_width);
        float height = res.getDimension(R.dimen.thumb_height);
        return ThumbUtils.createThumbFromImage(from, to, width, height);
    }

    public static String getUserImageLastModified(String userId) {
        File lastModified = FileManager.getUserImageLastModified(userId);
        if(!lastModified.exists()) {
            return null;
        }

        return FileUtils.toString(lastModified);
    }

    public static void setUserImageLastModified(String userId, String lastModifiedStr) {
        File lastModified = FileManager.getUserImageLastModified(userId);
        if(lastModifiedStr == null) {
            lastModified.delete();
        }
        else {
            FileUtils.writeStringToFile(lastModifiedStr, lastModified);
        }
    }
}
