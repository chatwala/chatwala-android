package com.chatwala.android.queue;

import com.chatwala.android.events.Event;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.Params;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/8/2014
 * Time: 4:10 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CwJob extends Job {
    private String eventId;

    /**
     * This only exists so that subclasses can override the default constructor.
     * The only reason we need to do that if for GSON. This should never get called.
     */
    protected CwJob() {
        super(new Params(1));
    }

    protected CwJob(CwJobParams params) {
        this(Event.Id.UNUSED, params);
    }

    protected CwJob(String eventId, CwJobParams params) {
        super(params);
        this.eventId = eventId;
    }

    protected String getEventId() {
        return eventId;
    }

    /**
     * This method returns whether multiple Jobs with the same UID are allowed to be in the
     * job queue concurrently.
     *
     * In general, it should not be overridden.
     *
     * @return whether this Job can have multiple instances with the same UID in the job queue
     * concurrently.
     */
    public boolean areMultipleInstancesAllowed() {
        return false;
    }

    /**
     * Returns a UID used to identify if a current job is running.
     * If a class/instance member is being returned, it must be instantiated by the time the constructor returns.
     * This will be called before any of the Job callbacks (onAdded, onRun, etc...).
     *
     * @return a UID represented as a String to uniquely identify this instance.
     */
    public abstract String getUID();

    protected abstract JobManager getQueueToPostTo();

    protected CwJob postMeToQueue() {
        JobQueues.postToQueue(this, getQueueToPostTo());
        return this;
    }

    protected void postMeToQueue(JobManager queueToPostTo) {
        JobQueues.postToQueue(this, queueToPostTo);
    }

    protected JobManager getApiQueue() {
        return JobQueues.getApiQueue();
    }

    protected JobManager getDownloadQueue() {
        return JobQueues.getDownloadQueue();
    }

    protected JobManager getUploadQueue() {
        return JobQueues.getUploadQueue();
    }

    @Override
    public void onAdded() {}

    @Override
    protected void onCancel() {}

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return true;
    }

    @Override
    protected int getRetryLimit() {
        return 10;
    }
}
