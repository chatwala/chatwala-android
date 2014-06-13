package com.chatwala.android.media;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/31/2014
 * Time: 10:23 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CwTrack {
    public String getUri();

    public long getDuration();

    public long getPlaybackOffset();

    public long getSize();
}
