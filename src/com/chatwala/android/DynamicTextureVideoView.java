package com.chatwala.android;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import com.chatwala.android.util.AndroidUtils;
import com.chatwala.android.util.CWLog;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/30/13
 * Time: 6:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class DynamicTextureVideoView extends TextureView implements TextureView.SurfaceTextureListener, MediaPlayer.OnBufferingUpdateListener
{
    private final int VIDEO_DISPLAY_DELAY = 100;
    public static final int GIVE_UP_THUMB = 5000;
    private final Handler handler;
    private File video;
    private boolean runStart;
    private int width;
    private int height;
    private int rotation;
    public MediaPlayer mMediaPlayer;
    private boolean initComplete;
    private boolean startCalled;
    VideoReadyCallback callback;

    public interface VideoReadyCallback
    {
        void ready();
    }

    public DynamicTextureVideoView(final Context context, File video, int width, int height, int rotation, VideoReadyCallback callback, final boolean runStart)
    {
        super(context);
        this.rotation = rotation;
        this.video = video;
        this.runStart = runStart;

        this.width = rotation == 180 ? height : width;
        this.height = rotation == 180 ? width : height;

        setSurfaceTextureListener(this);
        mMediaPlayer = new MediaPlayer();

        handler = new Handler();

        this.callback = callback;
        if (callback != null)
            callback.ready();

        if (isAvailable())
            onSurfaceTextureAvailable(getSurfaceTexture(), this.width, this.height);
//            setOnPreparedListener(new MediaPlayer.OnPreparedListener()
//            {
//                @Override
//                public void onPrepared(MediaPlayer mp)
//                {
//                    DynamicVideoView.this.mediaPlayer = mp;
//                    int volume = context.getResources().getInteger(R.integer.video_playback_volume);
//                    mediaPlayer.setVolume((float)volume/100f, (float)volume/100f);
//                }
//            });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (((ViewGroup) getParent()).getHeight() != 0)
        {
            //either scale up the height of the preview, so that is at least the size of the container
            int viewWidth = ((ViewGroup) getParent()).getWidth();
            int previewHeight = Math.max(width, height);
            int previewWidth = Math.min(width, height);
            double ratio = (double) viewWidth / (double) previewWidth;

            double newPreviewHeight = (double) previewHeight * ratio;
            double newPreviewWidth = (double) previewWidth * ratio;

            //Preview is rotated 90 degrees, so swap width/height
            setMeasuredDimension((int) newPreviewWidth, (int) newPreviewHeight);
        }
        else
        {
            //The surface needs a non-zero size for the callbacks to trigger
            setMeasuredDimension(1, 1);
        }
    }

    public void start()
    {
        if (mMediaPlayer == null)
            return;

        if (initComplete)
            mMediaPlayer.start();
        else
            startCalled = true;
    }

    public void pause()
    {
        if (mMediaPlayer == null)
            return;

        if (initComplete)
            mMediaPlayer.pause();
    }

    public void seekTo(int ms)
    {
        if (mMediaPlayer == null)
            return;

        if (initComplete)
            mMediaPlayer.seekTo(ms);
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener)
    {
        if (mMediaPlayer == null)
            return;

        mMediaPlayer.setOnCompletionListener(listener);
    }

    public boolean isPlaying()
    {
        return initComplete ? mMediaPlayer.isPlaying() : false;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        if (mMediaPlayer == null)
            return;

        Surface s = new Surface(surface);

        try
        {
            if (rotation == 180)
            {
                Matrix matrix = new Matrix();
                matrix.setRotate(90f, width / 2, height / 2);
                setTransform(matrix);
            }


            mMediaPlayer.setDataSource(video.getPath());
            mMediaPlayer.setSurface(s);
            mMediaPlayer.prepare();


            initComplete = true;
            if (startCalled)
            {
                mMediaPlayer.start();
                startCalled = false;
            }
            else if(runStart)
            {
                runningStart();
            }
            mMediaPlayer.setOnBufferingUpdateListener(this);
//               mMediaPlayer.setOnCompletionListener(this);
//               mMediaPlayer.setOnPreparedListener(this);
//               mMediaPlayer.setOnVideoSizeChangedListener(this);
//               mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private boolean isActivityActive()
        {
            return ((NewCameraActivity)getContext()).isActivityActive();
        }

    private void runningStart()
        {
            AndroidUtils.isMainThread();

            if(!isActivityActive())
                return;

            mMediaPlayer.setVolume(0f, 0f);
            start();
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    long start = System.currentTimeMillis();
                    while (System.currentTimeMillis() - start < GIVE_UP_THUMB)
                    {
                        try
                        {
                            Thread.sleep(70);
                        }
                        catch (InterruptedException e)
                        {
                        }

                        if(!isActivityActive())
                            return;

                        if (mMediaPlayer.getCurrentPosition() > 0)
                        {
                            break;
                        }
                    }
                    handler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            pause();
                            resetVolume(getContext());
                            mMediaPlayer.seekTo(0);
                            initDone();
                        }
                    });

                }
            }).start();
        }

    private void initDone()
        {
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    if(callback != null)
                        callback.ready();
                }
            }, VIDEO_DISPLAY_DELAY);
        }

    private void resetVolume(Context context)
        {
            int volume = context.getResources().getInteger(R.integer.video_playback_volume);
            mMediaPlayer.setVolume((float) volume / 100f, (float) volume / 100f);
        }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        mMediaPlayer.release();
        mMediaPlayer = null;

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
//        CWLog.i(DynamicTextureVideoView.class, "onSurfaceTextureUpdated");
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent)
    {
        CWLog.i(DynamicTextureVideoView.class, "onBufferingUpdate");
    }
}
