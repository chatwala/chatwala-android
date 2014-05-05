package com.chatwala.android.camera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import com.chatwala.android.util.Logger;

import java.io.IOException;

public class ChatwalaPlaybackTexture extends TextureView implements TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {
    private VideoMetadata metadata;
    private MediaPlayer mediaPlayer;
    private boolean loopPlayback = false;
    private boolean hasAudioFocus = false;
    private OnPlaybackReadyListener listener;

    public interface OnPlaybackReadyListener {
        public void onPlaybackReady();
    }

    public void forceOnMeasure() {
        requestLayout();
    }

    public ChatwalaPlaybackTexture(Context context, VideoMetadata metadata, boolean loopPlayback) throws IOException {
        super(context);
        this.metadata = metadata;
        this.loopPlayback = loopPlayback;

        initMediaPlayer();
        setSurfaceTextureListener(this);

        if(isAvailable()) {
            onSurfaceTextureAvailable(getSurfaceTexture(), getVideoWidth(), getVideoHeight());
        }
    }

    public void setOnPlaybackReadyListener(OnPlaybackReadyListener listener) {
        this.listener = listener;
    }

    private int getVideoWidth() {
        return metadata.getRotation() == 180 ? metadata.getHeight() : metadata.getWidth();
    }

    private int getVideoHeight() {
        return metadata.getRotation() == 180 ? metadata.getWidth() : metadata.getHeight();
    }

    private void initMediaPlayer() throws IOException {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(loopPlayback);
        mediaPlayer.setDataSource(metadata.getVideo().getPath());
        mediaPlayer.setOnPreparedListener(this);
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(listener);
        }
    }

    public boolean start() {
        if(mediaPlayer == null) {
            return false;
        }
        else {
            try {
                if(getContext() != null) {
                    AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                    int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        Logger.w("Couldn't get audio focus");
                    }
                    else {
                        hasAudioFocus = true;
                    }
                }
                mediaPlayer.start();
                return true;
            }
            catch(Exception e) {
                Logger.e("Starting the media player in a bad state", e);
                return false;
            }
        }
    }

    public void pause() {
        if(mediaPlayer != null) {
            try {
                mediaPlayer.pause();
            }
            catch(Exception e) {
                Logger.e("Pausing the media player in a bad state", e);
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
        try {
            if(mediaPlayer == null) {
                initMediaPlayer();
            }

            Surface s = new Surface(surface);

            if(metadata.getRotation() == 180) {
                Matrix matrix = new Matrix();
                matrix.setRotate(90f, width / 2, height / 2);
                setTransform(matrix);
            }

            mediaPlayer.setSurface(s);
            mediaPlayer.prepareAsync();
        }
        catch(Exception e) {
            Logger.e("Got an error when the playback texture became available", e);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if(getContext() != null && hasAudioFocus) {
            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            audioManager.abandonAudioFocus(this);
            hasAudioFocus = false;
        }

        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if(listener != null) {
            listener.onPlaybackReady();
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if(mediaPlayer == null) {
            try {
                initMediaPlayer();
            }
            catch(Exception e) {
                Logger.e("Couldn't init media player for when we got audio focus", e);
                return;
            }
        }

        switch(focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mediaPlayer.setVolume(1f, 1f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mediaPlayer.setVolume(0f, 0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mediaPlayer.setVolume(.5f, .5f);
                break;
            default:
                mediaPlayer.setVolume(1f, 1f);
        }
    }
}
