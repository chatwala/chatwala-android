package com.chatwala.android.events;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/9/2014
 * Time: 6:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProgressEvent extends Event {
    private int progress;


    public ProgressEvent(String id, int progress) {
        super(id);
        this.progress = progress;
    }

    public ProgressEvent(String id, long progress, long total) {
        super(id);
        this.progress = (int) (((float) progress / (float) total) * 100);
    }

    public int getProgress() {
        return progress;
    }
}
