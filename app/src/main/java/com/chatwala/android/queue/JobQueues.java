package com.chatwala.android.queue;

import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.events.ChatwalaMessageEvent;
import com.chatwala.android.events.ProgressEvent;
import com.chatwala.android.messages.ChatwalaMessageBase;
import com.staticbloc.jobs.JobQueue;
import com.staticbloc.jobs.JobQueueInitializer;
import com.staticbloc.jobs.SqlJobQueue;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Eliezer on 5/8/2014.
 */
public class JobQueues {
    private static final String API_JOB_QUEUE_ID = "CwApiQueue";
    private static final String DOWNLOAD_JOB_QUEUE_ID = "CwDownloadQueue";
    private static final String UPLOAD_JOB_QUEUE_ID = "CwUploadQueue";

    private ChatwalaApplication app;

    private JobQueue apiQueue;
    private JobQueue downloadQueue;
    private JobQueue uploadQueue;

    private JobQueues() {}

    private void initQueues() {
        Map<Type, SqlJobQueue.TypeAdapter> typeAdapters = new HashMap<Type, SqlJobQueue.TypeAdapter>();
        typeAdapters.put(ChatwalaMessageBase.class, new SqlJobQueue.TypeAdapter<ChatwalaMessageBase>());
        typeAdapters.put(ChatwalaMessageEvent.class, new SqlJobQueue.TypeAdapter<ChatwalaMessageEvent>());
        typeAdapters.put(ProgressEvent.class, new SqlJobQueue.TypeAdapter<ProgressEvent>());
        initApiQueue(typeAdapters);
        initDownloadQueue(typeAdapters);
        initUploadQueue(typeAdapters);
    }

    private static class Singleton {
        public static final JobQueues instance = new JobQueues();
    }

    public static void attachToApp(ChatwalaApplication app) {
        Singleton.instance.app = app;

        Singleton.instance.initQueues();
    }

    private void initApiQueue(Map<Type, SqlJobQueue.TypeAdapter> typeAdapters) {
        apiQueue = new SqlJobQueue(app.getApplicationContext(),
                new JobQueueInitializer()
                    .name(API_JOB_QUEUE_ID)
                    .shouldDebugLog(EnvironmentVariables.get().isDebug())
                    .minLiveConsumers(3)
                    .maxLiveConsumers(10)
                    .consumerKeepAliveSeconds(45), typeAdapters);
        apiQueue.start();
    }

    private void initDownloadQueue(Map<Type, SqlJobQueue.TypeAdapter> typeAdapters) {
        downloadQueue = new SqlJobQueue(app.getApplicationContext(),
                new JobQueueInitializer()
                        .name(DOWNLOAD_JOB_QUEUE_ID)
                        .shouldDebugLog(EnvironmentVariables.get().isDebug())
                        .minLiveConsumers(3)
                        .maxLiveConsumers(10)
                        .consumerKeepAliveSeconds(45), typeAdapters);
        downloadQueue.start();
    }

    private void initUploadQueue(Map<Type, SqlJobQueue.TypeAdapter> typeAdapters) {
        uploadQueue = new SqlJobQueue(app.getApplicationContext(),
                new JobQueueInitializer()
                        .name(UPLOAD_JOB_QUEUE_ID)
                        .shouldDebugLog(EnvironmentVariables.get().isDebug())
                        .minLiveConsumers(3)
                        .maxLiveConsumers(10)
                        .consumerKeepAliveSeconds(45), typeAdapters);
        uploadQueue.start();
    }

    /*package*/ static JobQueue getApiQueue() {
        return Singleton.instance.apiQueue;
    }

    /*package*/ static JobQueue getDownloadQueue() {
        return Singleton.instance.downloadQueue;
    }

    /*package*/ static JobQueue getUploadQueue() {
        return Singleton.instance.uploadQueue;
    }
}
