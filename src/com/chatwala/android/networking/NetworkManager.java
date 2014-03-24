package com.chatwala.android.networking;

import com.chatwala.android.AbsManager;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.CwResult;
import org.json.JSONObject;

import java.util.concurrent.Future;

public class NetworkManager extends AbsManager {
    private static final int NUM_THREADS = 5;

    private NetworkManager() {
        super(NUM_THREADS);
    }

    private static class Singleton {
        public static final NetworkManager instance = new NetworkManager();
    }

    public static NetworkManager attachToApp(ChatwalaApplication app) {
        Singleton.instance.attachToApplication(app);
        return Singleton.instance;
    }

    public static NetworkManager get() {
        return Singleton.instance;
    }

    public Future<CwResult<Response<JSONObject>>> getUserPictureUploadUrl(String userId) {
        return getQueue().submit(CwApi.getUserPictureUploadUrl(getApp(), userId));
    }

}
