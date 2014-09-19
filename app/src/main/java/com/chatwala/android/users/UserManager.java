package com.chatwala.android.users;

import com.chatwala.android.app.AppPrefs;
import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.util.Logger;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/10/2014
 * Time: 11:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserManager {
    private ChatwalaApplication app;
    private final ExecutorService queue;

    private ExecutorService getQueue() {
        return queue;
    }

    private ChatwalaApplication getApp() {
        return app;
    }

    private UserManager() {
        queue = Executors.newFixedThreadPool(1);
    }

    private static class Singleton {
        public static final UserManager instance = new UserManager();
    }

    private static UserManager me() {
        return Singleton.instance;
    }

    public static UserManager attachToApp(ChatwalaApplication app) {
        me().app = app;
        return me();
    }

    public static boolean isUserCreated() {
        return getUserId() != null;
    }

    public static void createUser() {
        String userId = UUID.randomUUID().toString();
        AppPrefs.setUserId(userId);
        Logger.i("User id is " + userId);
    }

    public static String getUserId() {
        return AppPrefs.getUserId();
    }

    public static String getUserProfilePicReadUrl() {
        return AppPrefs.getUserProfilePicReadUrl();
    }

    public static void setUserProfilePicReadUrl(String userProfilePicReadUrl) {
        AppPrefs.setUserProfilePicReadUrl(userProfilePicReadUrl);
    }

    public static String getUserProfilePicLastModified() {
        return AppPrefs.getUserProfilePicLastModified();
    }

    public static void setUserProfilePicLastModified(String userProfilePicReadUrlLastModified) {
        AppPrefs.setUserProfilePicLastModified(userProfilePicReadUrlLastModified);
    }
}
