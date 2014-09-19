package com.chatwala.android.queue;

import android.content.Context;
import android.content.SharedPreferences;
import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.util.Logger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.JobStatus;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.log.CustomLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by Eliezer on 5/8/2014.
 */
public class JobQueues {
    private static final String API_JOB_QUEUE_ID = "CwApiQueue";
    private static final String DOWNLOAD_JOB_QUEUE_ID = "CwDownloadQueue";
    private static final String UPLOAD_JOB_QUEUE_ID = "CwUploadQueue";

    private static final String JOBS_IN_FLIGHT_PREFS = "JOBS_IN_FLIGHT";
    private static final String JOBS_INFO_KEY = "JOBS_INFO";

    private ChatwalaApplication app;

    private JobManager apiQueue;
    private JobManager downloadQueue;
    private JobManager uploadQueue;

    private ExecutorService theQueuesQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "JobQueuePoster");
        }
    });
    private SharedPreferences sp;

    private Map<String, List<InFlightJobInfo>> jobsInFlight;

    private NetworkConnectionUtil networkConnectionUtil;
    private GsonJobSerializer gsonJobSerializer;

    private JobQueues() {
        jobsInFlight = new ConcurrentHashMap<String, List<InFlightJobInfo>>();
    }

    private void initQueues() {
        initApiQueue();
        initDownloadQueue();
        initUploadQueue();
    }

    private static class Singleton {
        public static final JobQueues instance = new JobQueues();
    }

    public static void attachToApp(ChatwalaApplication app) {
        Singleton.instance.app = app;
        Singleton.instance.sp = app.getSharedPreferences(JOBS_IN_FLIGHT_PREFS, Context.MODE_PRIVATE);

        Singleton.instance.jobsInFlight = Collections.synchronizedMap(Singleton.instance.getPersistedJobsInFlight());

        Singleton.instance.networkConnectionUtil = new NetworkConnectionUtil(app);
        Singleton.instance.gsonJobSerializer = new GsonJobSerializer();

        Singleton.instance.initQueues();
    }

    private void initApiQueue() {
        Configuration config = new Configuration.Builder(app)
                .id(API_JOB_QUEUE_ID)
                .customLogger(new CustomLogger() {
                    @Override
                    public boolean isDebugEnabled() {
                        return EnvironmentVariables.get().isDebug();
                    }

                    @Override
                    public void d(String s, Object... objects) {
                        Logger.d(API_JOB_QUEUE_ID, s, objects);
                    }

                    @Override
                    public void e(Throwable throwable, String s, Object... objects) {
                        Logger.e(API_JOB_QUEUE_ID, s, throwable, objects);
                    }

                    @Override
                    public void e(String s, Object... objects) {
                        Logger.e(API_JOB_QUEUE_ID, s, objects);
                    }
                })
                .networkUtil(networkConnectionUtil)
                .jobSerializer(gsonJobSerializer)
                .minConsumerCount(3)
                .loadFactor(1)
                .consumerKeepAlive(45)
                .build();
        apiQueue = new JobManager(app, config);
    }

    private void initDownloadQueue() {
        Configuration config = new Configuration.Builder(app)
                .id(DOWNLOAD_JOB_QUEUE_ID)
                .customLogger(new CustomLogger() {
                    @Override
                    public boolean isDebugEnabled() {
                        return EnvironmentVariables.get().isDebug();
                    }

                    @Override
                    public void d(String s, Object... objects) {
                        Logger.d(DOWNLOAD_JOB_QUEUE_ID, s, objects);
                    }

                    @Override
                    public void e(Throwable throwable, String s, Object... objects) {
                        Logger.e(DOWNLOAD_JOB_QUEUE_ID, s, throwable, objects);
                    }

                    @Override
                    public void e(String s, Object... objects) {
                        Logger.e(DOWNLOAD_JOB_QUEUE_ID, s, objects);
                    }
                })
                .networkUtil(networkConnectionUtil)
                .jobSerializer(gsonJobSerializer)
                .minConsumerCount(3)
                .loadFactor(1)
                .consumerKeepAlive(45)
                .build();
        downloadQueue = new JobManager(app, config);
    }

    private void initUploadQueue() {
        Configuration config = new Configuration.Builder(app)
                .id(UPLOAD_JOB_QUEUE_ID)
                .customLogger(new CustomLogger() {
                    @Override
                    public boolean isDebugEnabled() {
                        return EnvironmentVariables.get().isDebug();
                    }

                    @Override
                    public void d(String s, Object... objects) {
                        Logger.d(UPLOAD_JOB_QUEUE_ID, s, objects);
                    }

                    @Override
                    public void e(Throwable throwable, String s, Object... objects) {
                        Logger.e(UPLOAD_JOB_QUEUE_ID, s, throwable, objects);
                    }

                    @Override
                    public void e(String s, Object... objects) {
                        Logger.e(UPLOAD_JOB_QUEUE_ID, s, objects);
                    }
                })
                .networkUtil(networkConnectionUtil)
                .jobSerializer(gsonJobSerializer)
                .minConsumerCount(3)
                .loadFactor(1)
                .consumerKeepAlive(45)
                .build();
        uploadQueue = new JobManager(app, config);
    }

    /*package*/ static JobManager getApiQueue() {
        return Singleton.instance.apiQueue;
    }

    /*package*/ static JobManager getDownloadQueue() {
        return Singleton.instance.downloadQueue;
    }

    /*package*/ static JobManager getUploadQueue() {
        return Singleton.instance.uploadQueue;
    }

    /*package*/ static void postToQueue(final CwJob j, final JobManager queueToPostTo) {
        Singleton.instance.theQueuesQueue.execute(new Runnable() {
            @Override
            public void run() {
                Singleton.instance.postToQueueIfAllowed(j, queueToPostTo);
            }
        });
    }

    /*
    * used to be wrapped in a synchronized block locking on jobsInFlight
    * no longer need to because this method gets executed on a single threaded executor
    */
    private void postToQueueIfAllowed(final CwJob j, final JobManager queueToPostTo) {
        if(jobsInFlight.size() > 50) {
            trimJobsInFlight(queueToPostTo);
        }

        boolean oneIsRunning = false;
        String jobKey = j.getClass().getName() + "-" + (j.getUID() == null ? "" : j.getUID());
            List<InFlightJobInfo> jobInfos = jobsInFlight.get(jobKey);
            if(jobInfos == null) {
                jobInfos = new ArrayList<InFlightJobInfo>();
            }
            else {
                Iterator<InFlightJobInfo> it = jobInfos.listIterator();
                while(it.hasNext()) {
                    InFlightJobInfo jobInfo = it.next();
                    if(queueToPostTo.getJobStatus(jobInfo.getJobId(), j.isPersistent()) != JobStatus.UNKNOWN) {
                        oneIsRunning = true;
                    }
                    else {
                        it.remove();
                        Logger.i("Job Manager removed a finished " + jobInfo.getJobName() + " created at " + jobInfo.getCreationTimestamp());
                    }
                }
            }
            if(j.areMultipleInstancesAllowed() || !oneIsRunning) {
                long newId = queueToPostTo.addJob(j);
                InFlightJobInfo newJobInfo = new InFlightJobInfo(newId, j);
                jobInfos.add(newJobInfo);
                Logger.i("Job Manager added a new job - " + newJobInfo.getJobName());
            }
            else {
                InFlightJobInfo rejectedJobInfo = new InFlightJobInfo(-1, j);
                Logger.i("Job Manager rejected a new job (already in flight) - " + rejectedJobInfo.getJobName());
            }
            jobsInFlight.put(jobKey, jobInfos);
            persistJobsInFlight();
    }

    private void trimJobsInFlight(JobManager queueToPostTo) {
        List<String> keysToRemove = new ArrayList<String>();

        for(String key : jobsInFlight.keySet()) {
            List<InFlightJobInfo> jobInfos = jobsInFlight.get(key);
            if(jobInfos == null || jobInfos.size() == 0) {
                keysToRemove.add(key);
            }
            else {
                Iterator<InFlightJobInfo> it = jobInfos.listIterator();
                while(it.hasNext()) {
                    InFlightJobInfo jobInfo = it.next();
                    if(queueToPostTo.getJobStatus(jobInfo.getJobId(), jobInfo.isPersistent()) == JobStatus.UNKNOWN) {
                        it.remove();
                        Logger.i("Job Manager removed a finished " + jobInfo.getJobName() + " created at " + jobInfo.getCreationTimestamp());
                    }
                }
                if(jobInfos.size() == 0) {
                    keysToRemove.add(key);
                }
            }
        }

        for(String keyToRemove : keysToRemove) {
            jobsInFlight.remove(keyToRemove);
        }
    }

    private void persistJobsInFlight() {
        sp.edit().putString(JOBS_INFO_KEY, new Gson().toJson(jobsInFlight, jobsInFlight.getClass())).apply();
    }

    private Map<String, List<InFlightJobInfo>> getPersistedJobsInFlight() {
        return new Gson().fromJson(sp.getString(JOBS_INFO_KEY, "{}"), new TypeToken<Map<String, List<InFlightJobInfo>>>(){}.getType());
    }

    private static class InFlightJobInfo {
        private long jobId;
        private String creationTimestamp;
        private String jobName;
        private boolean isPersistent;

        private InFlightJobInfo(long jobId, CwJob job) {
            this.jobId = jobId;
            this.creationTimestamp = new Date(System.currentTimeMillis()).toString();
            this.jobName = job.getClass().getSimpleName() + (job.getUID() == null ? "" : "-" + job.getUID());
            this.isPersistent = job.isPersistent();
        }

        public long getJobId() {
            return jobId;
        }

        public String getCreationTimestamp() {
            return creationTimestamp;
        }

        public String getJobName() {
            return jobName;
        }

        public boolean isPersistent() {
            return isPersistent;
        }
    }
}
