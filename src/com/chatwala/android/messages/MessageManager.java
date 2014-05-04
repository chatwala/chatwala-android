package com.chatwala.android.messages;

import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.CWResult;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.networking.NetworkManager;
import com.chatwala.android.networking.requests.PutMessageThumbnailRequest;
import com.chatwala.android.superbus.server20.DeleteMessageCommand;
import com.chatwala.android.util.Logger;
import com.j256.ormlite.dao.Dao;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Eliezer on 3/27/2014.
 */
public class MessageManager {
    private static final int NUM_THREADS = 3;

    private static final String RECORDING_PREFIX = "recording_";
    private static final String RECORDING_FILE_EXTENSION = ".mp4";

    private ChatwalaApplication app;
    private final ExecutorService queue;

    private File tmpDir, outboxDir;

    private ExecutorService getQueue() {
        return queue;
    }

    private ChatwalaApplication getApp() {
        return app;
    }

    private MessageManager() {
        queue = Executors.newFixedThreadPool(NUM_THREADS);
        queue.execute(new Runnable() {
            @Override
            public void run() {
                loadFileStructure();
            }
        });
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

    private void loadFileStructure() {
        try {
            tmpDir = new File(getApp().getFilesDir(), "tmp");

            if(!tmpDir.exists()) {
                tmpDir.mkdir();
            }
        }
        catch(Exception e) {
            Logger.e("Couldn't laod the file structure", e);
        }
    }

    public Future<CWResult<Boolean>> uploadMessageThumbnail(final ChatwalaMessage message, final File thumbnailFile) {
        return getQueue().submit(new Callable<CWResult<Boolean>>() {
            @Override
            public CWResult<Boolean> call() throws Exception {
                NetworkManager networkManager = NetworkManager.getInstance();
                return networkManager.postToQueue(
                        new PutMessageThumbnailRequest(getApp(), message, thumbnailFile).getCallable(getApp(), 3)).get();
            }
        });
    }

    public Future<CWResult<Boolean>> deleteMessage(final String messageId) {
        return getQueue().submit(new Callable<CWResult<Boolean>>() {
            @Override
            public CWResult<Boolean> call() throws Exception {
                Dao<ChatwalaMessage, String> dao = DatabaseHelper.getInstance(getApp()).getChatwalaMessageDao();
                ChatwalaMessage message = dao.queryForId(messageId);
                message.setIsDeleted(true);
                dao.update(message);
                BusHelper.submitCommandAsync(getApp(), new DeleteMessageCommand(message));
                return new CWResult<Boolean>().setSuccess(true);
            }
        });
    }

    public File getNewRecordingFile() {
        return new File(tmpDir, RECORDING_PREFIX + System.currentTimeMillis() + RECORDING_FILE_EXTENSION);
    }
}
