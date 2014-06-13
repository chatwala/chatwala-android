package com.chatwala.android.media;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/30/2014
 * Time: 6:12 PM
 * To change this template use File | Settings | File Templates.
 */
/*package*/ class MediaPlayerState {
    /*package*/ final static int STATE_ERROR = -1;
    /*package*/ final static int STATE_IDLE = 0;
    /*package*/ final static int STATE_INIT = 1;
    /*package*/ final static int STATE_PREPARING = 2;
    /*package*/ final static int STATE_PREPARED = 3;
    /*package*/ final static int STATE_PLAYING = 4;
    /*package*/ final static int STATE_PAUSED = 5;
    /*package*/ final static int STATE_COMPLETED = 6;
    /*package*/ final static int STATE_STOPPED = 7;
    /*package*/ final static int STATE_RELEASED = -2;

    private int state = STATE_IDLE;

    public void setState(int state) {
        this.state = state;
    }

    public boolean isIdle() {
        return state == STATE_IDLE;
    }

    public boolean isInit() {
        return state == STATE_INIT;
    }

    public boolean isPreparing() {
        return state == STATE_PREPARING;
    }

    public boolean isPrepared() {
        return state == STATE_PREPARED;
    }

    public boolean isPlaying() {
        return state == STATE_PLAYING;
    }

    public boolean isPaused() {
        return state == STATE_PAUSED;
    }

    public boolean isCompleted() {
        return state == STATE_COMPLETED;
    }

    public boolean isStopped() {
        return state == STATE_STOPPED;
    }

    public boolean isError() {
        return state == STATE_ERROR;
    }

    public boolean isReleased() {
        return state == STATE_RELEASED;
    }
}
