package com.chatwala.android.messages;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.CWResult;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.camera.VideoMetadata;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.networking.NetworkManager;
import com.chatwala.android.networking.requests.PutMessageThumbnailRequest;
import com.chatwala.android.superbus.server20.DeleteMessageCommand;
import com.chatwala.android.util.FutureCallback;
import com.chatwala.android.util.Logger;
import com.j256.ormlite.dao.Dao;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

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
    }

    private static class Singleton {
        public static final MessageManager instance = new MessageManager();
    }

    public static MessageManager attachToApp(ChatwalaApplication app) {
        Singleton.instance.app = app;
        Singleton.instance.queue.execute(new Runnable() {
            @Override
            public void run() {
                Singleton.instance.loadFileStructure();
            }
        });
        return Singleton.instance;
    }

    public static MessageManager getInstance() {
        return Singleton.instance;
    }

    private <V> FutureTask<V> execute(final Callable<V> c, final FutureCallback<V> mainThreadCallback) {
        final FutureTask<V> f = new FutureTask<V>(c) {
            @Override
            protected void done() {
                super.done();
                if(mainThreadCallback != null) {
                    final FutureTask<V> innerF = this;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mainThreadCallback.runOnMainThread(innerF);
                        }
                    });
                }
            }
        };
        getQueue().execute(f);
        return f;
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

    public File getNewRecordingFile() {
        return new File(tmpDir, RECORDING_PREFIX + System.currentTimeMillis() + RECORDING_FILE_EXTENSION);
    }

    public FutureTask<VideoMetadata> getMessageVideoMetadata(final File recordedFile, FutureCallback<VideoMetadata> mainThreadCallback) {
        return execute(new Callable<VideoMetadata>() {
            @Override
            public VideoMetadata call() throws Exception {
                return VideoMetadata.parseVideoMetadata(recordedFile);
            }
        }, mainThreadCallback);
    }

    public FutureTask<Void> sendUnknownRecipientMessage(final File recordedFile) {
        return execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                IOUtils.copy(new FileInputStream(recordedFile), new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/test.mp4"));
                recordedFile.delete();
                return null;
            }
        }, null);
    }

    public FutureTask<Void> sendReply(final File recordedFile) {
        return execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                IOUtils.copy(new FileInputStream(recordedFile), new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/test.mp4"));
                recordedFile.delete();
                return null;
            }
        }, null);
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
}
