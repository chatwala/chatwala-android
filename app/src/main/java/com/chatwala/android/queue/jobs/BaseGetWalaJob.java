package com.chatwala.android.queue.jobs;

import com.chatwala.android.events.BaseChatwalaMessageEvent;
import com.chatwala.android.events.DrawerUpdateEvent;
import com.chatwala.android.events.Extras;
import com.chatwala.android.events.ProgressEvent;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.GetWalaRequest;
import com.chatwala.android.messages.ChatwalaMessageBase;
import com.chatwala.android.messages.MessageMetadataKeys;
import com.chatwala.android.messages.MessageState;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.NetworkConnectionChecker;
import com.chatwala.android.queue.Priority;
import com.chatwala.android.util.CwResult;
import com.chatwala.android.util.FileUtils;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.ZipUtils;
import com.j256.ormlite.dao.Dao;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.staticbloc.events.Events;
import com.staticbloc.jobs.JobInitializer;
import com.staticbloc.jobs.JobQueue;
import org.json.JSONObject;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/12/2014
 * Time: 1:57 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseGetWalaJob<T extends ChatwalaMessageBase> extends CwJob {
    private T message;
    private File downloadedFile = null;
    private Events events = Events.getDefault();

    protected BaseGetWalaJob() {}

    protected BaseGetWalaJob(String eventId, T message) {
        this(eventId, message, Priority.DOWNLOAD_HIGH_PRIORITY);
    }

    protected BaseGetWalaJob(String eventId, T message, int priority) {
        super(eventId, new JobInitializer()
                        .requiresNetwork(true)
                        .isPersistent(true)
                        .priority(priority));
        this.message = message;
    }

    protected Events getEvents() {
        return events;
    }

    @Override
    public String getUID() {
        return message.getMessageId();
    }

    @Override
    public int getInitialLockCount() {
        return 1;
    }

    @Override
    public void performJob() throws Throwable {
        if(downloadedFile != null && downloadedFile.exists()) {
            handleDownloadedMessage();
            return;
        }

        GetWalaRequest request = new GetWalaRequest<T>(message);
        request.log();
        String walaFilePath = message.getLocalWalaFile().getAbsolutePath();
        HttpClient.requestFile(request, walaFilePath, new AsyncHttpClient.FileCallback() {
            @Override
            public void onProgress(AsyncHttpResponse response, long downloaded, long total) {
                events.post(new ProgressEvent(getEventId(), downloaded, total));
            }

            @Override
            public void onCompleted(Exception e, AsyncHttpResponse rawResponse, File wala) {
                if (wala == null || !wala.exists()) {
                    raiseThrowableFromAsyncTask(new RuntimeException("Didn't get downloaded file"));
                    return;
                }

                if (e != null) {
                    if (wala.exists()) {
                        wala.delete();
                    }
                    raiseThrowableFromAsyncTask(e);
                    return;
                }

                CwHttpResponse<Void> response = new CwHttpResponse<Void>(rawResponse);
                NetworkLogger.log(response, null);

                if (response.getResponseCode() == 404) {
                    if (wala.exists()) {
                        wala.delete();
                    }
                    notifyAsyncTaskDone();
                    events.post(createMessageEvent(getEventId(), Extras.WALA_BAD_SHARE_ID));
                    return;
                }

                try {
                    downloadedFile = wala;
                    handleDownloadedMessage();
                    notifyAsyncTaskDone();
                } catch (Throwable innerE) {
                    Logger.e("There was an error handling the downloaded wala", innerE);
                    raiseThrowableFromAsyncTask(innerE);
                }
            }
        }, HttpClient.SHORTER_FILE_TIMEOUT);
    }

    private void handleDownloadedMessage() throws Throwable {
        Dao<T, String> dao = message.getDao();
        File parent = downloadedFile.getParentFile();
        ZipUtils.unzipFiles(downloadedFile, parent);
        File videoFile = message.getLocalVideoFile();
        File metadataFile = message.getLocalMetadataFile();

        downloadedFile.delete();

        T dbMessage = dao.queryForId(message.getMessageId());
        if(dbMessage == null) {
            JSONObject metadata = new JSONObject("{}");
            if(metadataFile.exists()) {
                metadata = new JSONObject(FileUtils.toString(metadataFile));
            }

            dbMessage = message;

            if(!dbMessage.getMessageId().equals(metadata.getString(MessageMetadataKeys.ID))) {
                deleteMessage();
                return;
            }
            dbMessage.setMessageState(MessageState.UNREAD);
            try {
                dbMessage.populateFromMetadata(metadata);
            }
            catch(Exception e) {
                Logger.e("Had some trouble populating the message from the metadata", e);
            }
            dbMessage.setWalaDownloaded(true);
            dao.createOrUpdate(dbMessage);
        }
        else if(!dbMessage.isWalaDownloaded()) {
            dbMessage.setWalaDownloaded(true);
            dao.update(dbMessage);
        }

        File messageThumb = message.getLocalMessageThumb();
        if(messageThumb.exists()) {
            messageThumb.setLastModified(0);
        }

        onWalaDownloaded(message);
        events.post(createMessageEvent(getEventId(), new CwResult<T>(message)));

        events.post(new DrawerUpdateEvent(DrawerUpdateEvent.LOAD_EVENT_EXTRA));

        //we might be redownloading this wala and already have the message thumb
        if(!message.getLocalMessageThumb().exists()) {
            GetMessageImageJob.post(message);
        }
        GetUserImageJob.post(message.getSenderId(), message.getUserImageUrl());
    }

    protected final T getMessage() {
        return message;
    }

    protected abstract BaseChatwalaMessageEvent<T> createMessageEvent(String eventId, CwResult<T> result);

    protected abstract BaseChatwalaMessageEvent<T> createMessageEvent(String eventId, int extra);

    protected abstract void onWalaDownloaded(T message);

    protected abstract void deleteMessage();

    @Override
    public void onCanceled() {
        events.post(createMessageEvent(getEventId(), Extras.CANCELED));
    }

    @Override
    protected JobQueue getQueueToPostTo() {
        return getDownloadQueue();
    }

    @Override
    public boolean canReachRequiredNetwork() {
        return NetworkConnectionChecker.getInstance().isConnected();
    }
}
