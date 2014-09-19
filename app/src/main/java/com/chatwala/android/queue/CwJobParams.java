package com.chatwala.android.queue;

import com.path.android.jobqueue.Params;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/30/2014
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class CwJobParams extends Params {
    public CwJobParams(Priority priority) {
        super(priority.getPriority());
    }

    public CwJobParams requireNetwork() {
        super.requireNetwork();
        return this;
    }

    public CwJobParams groupBy(String groupId) {
        super.groupBy(groupId);
        return this;
    }

    public CwJobParams persist() {
        super.persist();
        return this;
    }

    public CwJobParams delayInMs(long delayMs) {
        super.delayInMs(delayMs);
        return this;
    }

    public CwJobParams setRequiresNetwork(boolean requiresNetwork) {
        super.setRequiresNetwork(requiresNetwork);
        return this;
    }

    public CwJobParams setGroupId(String groupId) {
        super.setGroupId(groupId);
        return this;
    }

    public CwJobParams setPersistent(boolean persistent) {
        super.setPersistent(persistent);
        return this;
    }

    public CwJobParams setDelayMs(long delayMs) {
        super.setDelayMs(delayMs);
        return this;
    }
}
