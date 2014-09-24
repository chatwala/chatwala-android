package com.chatwala.android.queue;

import com.chatwala.android.events.Ids;
import com.staticbloc.jobs.BasicJob;
import com.staticbloc.jobs.JobInitializer;
import com.staticbloc.jobs.JobQueue;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/8/2014
 * Time: 4:10 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CwJob extends BasicJob {
    private String eventId;

    /**
     * This only exists so that subclasses can override the default constructor.
     * The only reason we need to do that if for GSON. This should never get called.
     */
    protected CwJob() {
        super(new JobInitializer());
    }

    protected CwJob(JobInitializer initializer) {
        this(Ids.UNUSED, initializer);
    }

    protected CwJob(String eventId, JobInitializer initializer) {
        super(initializer);
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

    protected abstract JobQueue getQueueToPostTo();

    protected CwJob postMeToQueue() {
        getQueueToPostTo().add(this);
        return this;
    }

    protected void postMeToQueue(JobQueue queueToPostTo) {
        queueToPostTo.add(this);
    }

    protected JobQueue getApiQueue() {
        return JobQueues.getApiQueue();
    }

    protected JobQueue getDownloadQueue() {
        return JobQueues.getDownloadQueue();
    }

    protected JobQueue getUploadQueue() {
        return JobQueues.getUploadQueue();
    }
}
