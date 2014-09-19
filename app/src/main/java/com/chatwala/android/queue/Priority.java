package com.chatwala.android.queue;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/30/2014
 * Time: 1:05 PM
 * To change this template use File | Settings | File Templates.
 */
public enum Priority {
    API_LOW_PRIORITY(1),
    API_MID_PRIORITY(2),
    API_HIGH_PRIORITY(3),
    API_IMMEDIATE_PRIORITY(4),
    DOWNLOAD_LOW_PRIORITY(5),
    DOWNLOAD_MID_PRIORITY(6),
    DOWNLOAD_HIGH_PRIORITY(7),
    DOWNLOAD_IMMEDIATE_PRIORITY(8),
    UPLOAD_LOW_PRIORITY(9),
    UPLOAD_MID_PRIORITY(10),
    UPLOAD_HIGH_PRIORITY(11),
    UPLOAD_IMMEDIATE_PRIORITY(12);

    private int priority;

    private Priority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
