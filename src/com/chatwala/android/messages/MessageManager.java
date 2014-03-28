package com.chatwala.android.messages;

import com.chatwala.android.CWResult;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.networking.NetworkManager;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Eliezer on 3/27/2014.
 */
public class MessageManager {
    private static final int NUM_THREADS = 3;

    private ChatwalaApplication app;
    private final ExecutorService queue;

    private final ExecutorService getQueue() {
        return queue;
    }

    private final ChatwalaApplication getApp() {
        return app;
    }

    private MessageManager() {
        queue = Executors.newFixedThreadPool(NUM_THREADS);
    }

    private static class Singleton {
        public static final MessageManager instance = new MessageManager();
    }

    public static MessageManager attachToApp(ChatwalaApplication app) {
        Singleton.instance.app = app;
        return Singleton.instance;
    }

    public static MessageManager getInstance() {
        return Singleton.instance;
    }

    public Future<CWResult<Boolean>> uploadMessageThumbnail(final URL writeUrl, final ChatwalaMessage message, final File thumbnailFile) {
        return getQueue().submit(new Callable<CWResult<Boolean>>() {
            @Override
            public CWResult<Boolean> call() throws Exception {
                NetworkManager networkManager = NetworkManager.getInstance();
                Future<CWResult<Boolean>> success;
                success = networkManager.putMessageThumbnail(writeUrl, thumbnailFile);
                if(!success.get().isSuccess()) { //we need to get a new write url
                    CWResult<URL> getWriteUrlResult = networkManager.getMessageThumbnailWriteUrl(message).get();
                    if(getWriteUrlResult.isSuccess()) {
                        return networkManager.putMessageThumbnail(getWriteUrlResult.getResult(), thumbnailFile).get();
                    }
                    else {
                        return new CWResult<Boolean>(getWriteUrlResult);
                    }
                }
                else {
                    return success.get();
                }
            }
        });
    }
}
