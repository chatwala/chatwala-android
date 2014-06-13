package com.chatwala.android.media;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/30/2014
 * Time: 5:48 PM
 * To change this template use File | Settings | File Templates.
 */
public interface OnTrackReadyListener {
    public boolean onInitialTrackReady(CwTrack track);

    public boolean onTrackReady(CwTrack track);

    public boolean onTracksFinished();

    public void onTrackError();
}
