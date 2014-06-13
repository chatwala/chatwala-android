package com.chatwala.android.camera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import com.chatwala.android.media.CwMediaPlayer;
import com.chatwala.android.media.CwTrack;
import com.chatwala.android.media.CwVideoTrack;
import com.chatwala.android.media.OnTrackReadyListener;
import com.chatwala.android.util.Logger;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaPlaybackTexture extends TextureView implements TextureView.SurfaceTextureListener,
        OnTrackReadyListener {
    public static final int FLAG_AUTO_START = 1;
    public static final int FLAG_LOOP_TRACKS = 2;
    public static final int FLAG_AUTO_ADVANCE = 4;
    public static final int FLAG_CROP_VIDEO = 8;

    private ArrayList<CwTrack> tracks;
    private CwMediaPlayer mediaPlayer;

    private int flags;

    private boolean isMuted = false;
    private OnTrackReadyListener onTrackReadyListener;

    public ChatwalaPlaybackTexture(Context context) {
        super(context);
    }

    public ChatwalaPlaybackTexture(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChatwalaPlaybackTexture(Context context, AttributeSet attrs, int theme) {
        super(context, attrs, theme);
    }

    public ChatwalaPlaybackTexture(Context context, CwTrack track, OnTrackReadyListener onTrackReadyListener) throws IOException {
        super(context);
        ArrayList<CwTrack> tracks = new ArrayList<CwTrack>(1);
        tracks.add(track);
        init(tracks, onTrackReadyListener, 0);
    }

    public ChatwalaPlaybackTexture(Context context, ArrayList<CwTrack> tracks, OnTrackReadyListener onTrackReadyListener) throws IOException {
        super(context);
        init(tracks, onTrackReadyListener, 0);
    }

    public void init(CwTrack track, OnTrackReadyListener onTrackReadyListener) throws IOException, IllegalStateException {
        init(track, onTrackReadyListener, 0);
    }

    public void init(CwTrack track, OnTrackReadyListener onTrackReadyListener, int flags) throws IOException, IllegalStateException {
        ArrayList<CwTrack> tracks = new ArrayList<CwTrack>(1);
        tracks.add(track);
        init(tracks, onTrackReadyListener, flags);
    }

    public void init(ArrayList<CwTrack> tracks, OnTrackReadyListener onTrackReadyListener) throws IOException, IllegalStateException {
        init(tracks, onTrackReadyListener, 0);
    }

    public void init(ArrayList<CwTrack> tracks, OnTrackReadyListener onTrackReadyListener, int flags) throws IOException, IllegalStateException {
        this.tracks = tracks;
        this.onTrackReadyListener = onTrackReadyListener;
        this.flags = flags;
        mediaPlayer = new CwMediaPlayer(tracks, this);
        mediaPlayer.init(shouldAutoStart(), shouldLoopTracks(), shouldCropVideo());

        setSurfaceTextureListener(this);

        if(isAvailable()) {
            onSurfaceTextureAvailable(getSurfaceTexture(), getVideoWidth(), getVideoHeight());
        }
    }

    public void setOnTrackReadyListener(OnTrackReadyListener onTrackReadyListener) {
        this.onTrackReadyListener = onTrackReadyListener;
    }

    private int getVideoWidth() {
        CwVideoTrack initialTrack = (CwVideoTrack) (tracks == null ? null : tracks.isEmpty() ? null : tracks.get(0));
        if(initialTrack == null) {
            return 1;
        }
        else {
            return initialTrack.getRotation() == 180 ? initialTrack.getWidth() : initialTrack.getHeight();
        }
    }

    private int getVideoHeight() {
        CwVideoTrack initialTrack = (CwVideoTrack) (tracks == null ? null : tracks.isEmpty() ? null : tracks.get(0));
        if(initialTrack == null) {
            return 1;
        }
        else {
            return initialTrack.getRotation() == 180 ? initialTrack.getHeight() : initialTrack.getWidth();
        }
    }

    public boolean setVolume(float left, float right) {
        if(mediaPlayer != null) {
            return mediaPlayer.setVolume(left, right);
        }
        else {
            return false;
        }
    }

    public boolean mute() {
        isMuted = true;
        if(mediaPlayer != null) {
            return mediaPlayer.mute();
        }
        else {
            return false;
        }
    }

    public boolean unmute() {
        isMuted = false;
        if(mediaPlayer != null) {
            return mediaPlayer.unmute();
        }
        else {
            return false;
        }
    }

    public boolean isMuted() {
        return isMuted;
    }

    public boolean isPlaying() {
        if(mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        else {
            return false;
        }
    }

    public boolean start() {
        if(mediaPlayer != null) {
            return mediaPlayer.start();
        }
        else {
            return false;
        }
    }

    public boolean pause() {
        if(mediaPlayer != null) {
            return mediaPlayer.pause();
        }
        else {
            return false;
        }
    }

    public boolean stop() {
        if(mediaPlayer != null) {
            return mediaPlayer.stop();
        }
        else {
            return false;
        }
    }

    public boolean seekTo(int millis) {
        if(mediaPlayer != null) {
            return mediaPlayer.seekTo(millis);
        }
        else {
            return false;
        }
    }

    public void reset() {
        if(mediaPlayer == null) {
            try {
                mediaPlayer = new CwMediaPlayer(tracks, this);
            }
            catch(Exception e) {
                Logger.e("Couldn't init the media player", e);
            }
        }
        else {
            try {
                mediaPlayer.reset(tracks);
            }
            catch(Exception e) {
                Logger.e("Couldn't reset the media player", e);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getParent() != null) {
            ViewGroup parent = ((ViewGroup) getParent());
            if(parent.getHeight() != 0) {
                int viewWidth = ((ViewGroup) getParent()).getWidth();
                int previewHeight = Math.max(getVideoWidth(), getVideoHeight());
                int previewWidth = Math.min(getVideoWidth(), getVideoHeight());
                double ratio = (double) viewWidth / (double) previewWidth;

                double newPreviewHeight = (double) previewHeight * ratio;
                double newPreviewWidth = (double) previewWidth * ratio;

                //Preview is rotated 90 degrees, so swap width/height
                setMeasuredDimension((int) newPreviewWidth, (int) newPreviewHeight);
            }
            else {
                //The surface needs a non-zero size for the callbacks to trigger
                setMeasuredDimension(1, 1);
            }
        }
        else {
            //The surface needs a non-zero size for the callbacks to trigger
            setMeasuredDimension(1, 1);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Surface s = new Surface(getSurfaceTexture());

        CwVideoTrack initialTrack = (CwVideoTrack) (tracks == null ? null : tracks.isEmpty() ? null : tracks.get(0));
        if(initialTrack != null && initialTrack.getRotation() == 180) {
            Matrix matrix = new Matrix();
            matrix.setRotate(90f, getWidth() / 2, getHeight() / 2);
            setTransform(matrix);
        }

        mediaPlayer.setSurface(s);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if(mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
        }
        mediaPlayer = null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}

    @Override
    public boolean onInitialTrackReady(CwTrack track) {
        if(onTrackReadyListener != null) {
            return onTrackReadyListener.onInitialTrackReady(track);
        }
        else {
            return shouldAutoStart();
        }
    }

    @Override
    public boolean onTrackReady(CwTrack track) {
        if(onTrackReadyListener != null) {
            return onTrackReadyListener.onTrackReady(track);
        }
        else {
            return shouldAutoAdvance();
        }
    }

    @Override
    public boolean onTracksFinished() {
        if(onTrackReadyListener != null) {
            boolean shouldLoop = onTrackReadyListener.onTracksFinished();
            if(!shouldLoop) {
                mediaPlayer.stop();
            }
            return shouldLoop;
        }
        else {
            boolean shouldLoop = shouldLoopTracks();
            if(!shouldLoop) {
                mediaPlayer.stop();
            }
            return shouldLoop;
        }
    }

    @Override
    public void onTrackError() {
        if(onTrackReadyListener != null) {
            onTrackReadyListener.onTrackError();
        }
        mediaPlayer.stop();
    }

    public boolean shouldAutoStart() {
        return FLAG_AUTO_START == (FLAG_AUTO_START & flags);
    }

    public boolean shouldLoopTracks() {
        return FLAG_LOOP_TRACKS == (FLAG_LOOP_TRACKS & flags);
    }

    public boolean shouldAutoAdvance() {
        return FLAG_AUTO_ADVANCE == (FLAG_AUTO_ADVANCE & flags);
    }

    public boolean shouldCropVideo() {
        return FLAG_CROP_VIDEO == (FLAG_CROP_VIDEO & flags);
    }

    public void setShouldCropVideo(boolean shouldCropVideo) {
        if(shouldCropVideo) {
            addFlag(FLAG_CROP_VIDEO);
        }
        else {
            clearFlag(FLAG_CROP_VIDEO);
        }
        mediaPlayer.setShouldCropVideo(shouldCropVideo);
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void addFlag(int flag) {
        flags |= flag;
    }

    public void clearFlags() {
        flags = 0;
    }

    public void clearFlag(int flag) {
        flags &= ~flag;
    }
}
