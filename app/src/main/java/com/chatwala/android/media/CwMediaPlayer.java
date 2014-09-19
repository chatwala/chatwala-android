package com.chatwala.android.media;

import android.annotation.TargetApi;
import android.media.MediaPlayer;
import android.os.Build;
import android.view.Surface;
import com.chatwala.android.util.Logger;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/30/2014
 * Time: 5:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class CwMediaPlayer {
    private static final int INVALID_TRACK = -1;

    private MediaPlayer mediaPlayer;
    private ArrayList<CwTrack> tracks;

    private boolean initted = false;
    private boolean shouldAutoStart = false;
    private boolean shouldLoopTracks = false;
    private boolean shouldCropVideo = false;

    private MediaPlayerState state = new MediaPlayerState();
    private int currentTrack = INVALID_TRACK;

    private OnTrackReadyListener onTrackReadyListener;

    public CwMediaPlayer() {
        this(null, null);
    }

    public CwMediaPlayer(ArrayList<CwTrack> tracks) {
        this(tracks, null);
    }

    public CwMediaPlayer(ArrayList<CwTrack> tracks, OnTrackReadyListener onTrackReadyListener) {
        mediaPlayer = new MediaPlayer();
        this.tracks = tracks;
        this.onTrackReadyListener = onTrackReadyListener;
    }

    public boolean init() throws IOException, IllegalStateException {
        return init(false, false, false);
    }

    public boolean init(boolean shouldAutoStart, boolean shouldLoopTracks, boolean shouldCropVideo) throws IOException, IllegalStateException {
        if(initted) {
            throw new IllegalStateException("This media player was already initted. Call reset or dispose of the object.");
        }
        initted = true;
        this.shouldAutoStart = shouldAutoStart;
        this.shouldLoopTracks = shouldLoopTracks;
        this.shouldCropVideo = shouldCropVideo;
        return readyNextTrack();
    }

    public void setOnTrackReadyListener(OnTrackReadyListener onTrackReadyListener) {
        this.onTrackReadyListener = onTrackReadyListener;
    }

    private boolean readyNextTrack() throws IOException {
        if(tracks == null || tracks.isEmpty()) {
            return false;
        }

        currentTrack++;
        if(currentTrack >= tracks.size()) {
            return false;
        }

        mediaPlayer.reset();

        mediaPlayer.setOnErrorListener(internalOnErrorListener);
        mediaPlayer.setOnPreparedListener(internalOnPreparedListener);
        mediaPlayer.setOnCompletionListener(internalOnCompletionListener);

        CwTrack nextTrack = getCurrentTrack();
        mediaPlayer.setDataSource(nextTrack.getUri());

        if(shouldCropVideo) {
            setShouldCropVideo(true);
        }

        state.setState(MediaPlayerState.STATE_PREPARING);
        mediaPlayer.prepareAsync();
        return true;
    }

    public boolean setTrack(CwTrack track) throws IOException {
        if(!state.isIdle() || track == null) {
            return false;
        }
        else {
            tracks = new ArrayList<CwTrack>();
            tracks.add(track);
            return readyNextTrack();
        }
    }

    public boolean setTracks(ArrayList<CwTrack> tracks) throws IOException {
        if(!state.isIdle() || tracks == null || tracks.isEmpty()) {
            return false;
        }
        else {
            this.tracks = tracks;
            return readyNextTrack();
        }
    }

    public CwTrack getCurrentTrack() {
        if(currentTrack >= 0 && currentTrack < tracks.size()) {
            return tracks.get(currentTrack);
        }
        else {
            return null;
        }
    }

    public void setShouldAutoStart(boolean shouldAutoStart) {
        this.shouldAutoStart = shouldAutoStart;
    }

    public void setShouldLoopTracks(boolean shouldLoopTracks) {
        this.shouldLoopTracks = shouldLoopTracks;
    }

    public void setShouldCropVideo(boolean shouldCropVideo) {
        this.shouldCropVideo = shouldCropVideo;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int videoMode = (shouldCropVideo ? MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING :
                    MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            setVideoScaleMode(videoMode);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public boolean setVideoScaleMode(int mode) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && !state.isReleased() && !state.isError() && !state.isIdle()) {
            mediaPlayer.setVideoScalingMode(mode);
            return true;
        }
        else {
            return false;
        }
    }

    public void setSurface(Surface surface) {
        mediaPlayer.setSurface(surface);
    }

    public boolean setVolume(float left, float right) {
        if(!state.isReleased() && !state.isError()) {
            mediaPlayer.setVolume(left, right);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean mute() {
        return setVolume(0, 0);
    }

    public boolean unmute() {
        return setVolume(1, 1);
    }

    public boolean start() {
        if(state.isPrepared() || state.isPaused() || state.isCompleted()) {
            if(getCurrentTrack().getPlaybackOffset() > 0) {
                seekTo((int) getCurrentTrack().getPlaybackOffset());
            }
            mediaPlayer.start();
            state.setState(MediaPlayerState.STATE_PLAYING);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean seekTo(int millis) {
        if(state.isPrepared() || state.isPlaying() || state.isPaused() || state.isCompleted()) {
            mediaPlayer.seekTo(millis);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean pause() {
        if(state.isPlaying() || state.isCompleted()) {
            mediaPlayer.pause();
            state.setState(MediaPlayerState.STATE_PAUSED);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean stop() {
        if(state.isPrepared() || state.isPlaying() || state.isPaused() || state.isCompleted()) {
            mediaPlayer.stop();
            state.setState(MediaPlayerState.STATE_STOPPED);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean isPlaying() {
        if(!state.isError()) {
            return state.isPlaying();
        }
        else {
            return false;
        }
    }

    public void reset() {
        reset(null);
    }

    public void reset(ArrayList<CwTrack> tracks) {
        mediaPlayer.reset();
        state = new MediaPlayerState();
        currentTrack = INVALID_TRACK;
        this.tracks = tracks;
        initted = false;
        shouldAutoStart = false;
        shouldLoopTracks = false;
    }

    public void release() {
        mediaPlayer.release();
        state.setState(MediaPlayerState.STATE_RELEASED);
    }

    private MediaPlayer.OnPreparedListener internalOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            state.setState(MediaPlayerState.STATE_PREPARED);
            if(!hasError()) {
                CwTrack track = getCurrentTrack();
                if(track == null) { //we've gotten to the end of the track list
                    if(onTrackReadyListener == null) {
                        if(shouldLoopTracks) {
                            loopTracks();
                        }
                    }
                    else {
                        if(onTrackReadyListener.onTracksFinished()) {
                            if(!loopTracks()) {
                                onTrackReadyListener.onTrackError();
                            }
                        }
                    }
                }
                else {
                    if(onTrackReadyListener == null) {
                        if(shouldAutoStart) {
                            start();
                        }
                    }
                    else {
                        if(currentTrack == 0) {
                            if(onTrackReadyListener.onInitialTrackReady(track)) {
                                if(!start()) {
                                    onTrackReadyListener.onTrackError();
                                }
                            }
                        }
                        else {
                            if(onTrackReadyListener.onTrackReady(track)) {
                                if(!start()) {
                                    onTrackReadyListener.onTrackError();
                                }
                            }
                        }
                    }
                }
            }
            else {
                if(onTrackReadyListener != null) {
                    onTrackReadyListener.onTrackError();
                }
            }
        }
    };

    private boolean loopTracks() {
        //set this to invalid track. the next call to readyNextTrack will set it to 0
        currentTrack = INVALID_TRACK;
        try {
            return readyNextTrack();
        }
        catch(IOException e) {
            return false;
        }
    }

    private MediaPlayer.OnCompletionListener internalOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if(!state.isError()) {
                try {
                    if(!readyNextTrack()) {
                        if(onTrackReadyListener == null) {
                            if(shouldLoopTracks) {
                                loopTracks();
                            }
                        }
                        else {
                            if(onTrackReadyListener.onTracksFinished()) {
                                if(!loopTracks()) {
                                    onTrackReadyListener.onTrackError();
                                }
                            }
                        }
                    }
                }
                catch(IOException e) {
                    state.setState(MediaPlayerState.STATE_ERROR);
                    if(onTrackReadyListener != null) {
                        onTrackReadyListener.onTrackError();
                    }
                }
            }
            else { //can get in this state if onError is called (since it returns false)
                if(onTrackReadyListener != null) {
                    onTrackReadyListener.onTrackError();
                }
            }
        }
    };

    public boolean hasError() {
        return state.isError();
    }

    private MediaPlayer.OnErrorListener internalOnErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
            state.setState(MediaPlayerState.STATE_ERROR);
            StringBuilder sb = new StringBuilder("CwVideoPlayer error");
            if(what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
                sb.append("UNKNOWN");
            }
            else if(what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                sb.append("SERVER_DIED");
            }
            if(extra == MediaPlayer.MEDIA_ERROR_IO) {
                sb.append("\nThere was an IO error");
            }
            else if(extra == MediaPlayer.MEDIA_ERROR_MALFORMED) {
                sb.append("\nThe stream was malformed");
            }
            else if(extra == MediaPlayer.MEDIA_ERROR_UNSUPPORTED) {
                sb.append("\nMediaPlayer does not support this stream");
            }
            else if(extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                sb.append("\nAn operation timed out");
            }
            else {
                sb.append("\nThere was an unknown error");
            }
            Logger.e(sb.toString());
            return false;
        }
    };
}
