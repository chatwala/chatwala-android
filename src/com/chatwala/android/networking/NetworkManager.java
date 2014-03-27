package com.chatwala.android.networking;

import com.chatwala.android.CWResult;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.networking.requests.UserPictureUploadUrlRequest;
import com.chatwala.android.util.Logger;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NetworkManager {
    private static final String clientId = "58041de0bc854d9eb514d2f22d50ad4c";
    private static final String clientSecret = "ac168ea53c514cbab949a80bebe09a8a";

    private static final int NUM_THREADS = 5;

    private ChatwalaApplication app;
    private final ExecutorService queue;

    private final ExecutorService getQueue() {
        return queue;
    }

    private final ChatwalaApplication getApp() {
        return app;
    }

    private NetworkManager() {
        queue = Executors.newFixedThreadPool(NUM_THREADS);
    }

    private static class Singleton {
        public static final NetworkManager instance = new NetworkManager();
    }

    public static NetworkManager attachToApp(ChatwalaApplication app) {
        Singleton.instance.app = app;
        return Singleton.instance;
    }

    public static NetworkManager getInstance() {
        return Singleton.instance;
    }

    public Future<CWResult<JSONObject>> getUserPictureUploadUrl(String userId) {
        return getQueue().submit(new UserPictureUploadUrlRequest(userId).getCallable(getApp(), 3));
    }

    public void setChatwalaHeaders(HttpURLConnection client) {
        try {
            String versionName = getApp().getPackageManager().getPackageInfo(getApp().getPackageName(), 0).versionName;
            client.setRequestProperty("x-chatwala", clientId + ":" + clientSecret);
            client.setRequestProperty("x-chatwala-appversion", versionName);
        }
        catch (Exception e) {
            Logger.e("Couldn't set chatwala headers", e);
        }
    }

}
