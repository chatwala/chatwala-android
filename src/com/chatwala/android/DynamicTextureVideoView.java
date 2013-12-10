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
    public static final int GIVE_UP_THUMB = 5000;
    private final Handler handler;
    private File video;
    private int width;
    private int height;
    private int rotation;
    public MediaPlayer mMediaPlayer;
    private boolean initComplete;
    private boolean startCalled;

    public interface InitCallback
    {
        void initCalled();
    }

    public DynamicTextureVideoView(final Context context, File video, int width, int height, int rotation)
    {
        super(context);
        this.rotation = rotation;
        this.video = video;

        this.width = rotation == 180 ? height : width;
        this.height = rotation == 180 ? width : height;

        handler = new Handler();
        setSurfaceTextureListener(this);
        mMediaPlayer = new MediaPlayer();
        if (isAvailable())
            onSurfaceTextureAvailable(getSurfaceTexture(), this.width, this.height);
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
        if (initComplete)
            mMediaPlayer.start();
        else
            startCalled = true;
    }

    public void pause()
    {
        if (initComplete)
            mMediaPlayer.pause();
    }

    public void seekTo(int ms)
    {
        if (initComplete)
            mMediaPlayer.seekTo(ms);
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener)
    {
        mMediaPlayer.setOnCompletionListener(listener);
    }

    public boolean isPlaying()
    {
        return initComplete ? mMediaPlayer.isPlaying() : false;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
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

            mMediaPlayer.start();
            mMediaPlayer.setVolume(0f, 0f);
            setVisibility(View.INVISIBLE);
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    long start = System.currentTimeMillis();
                    while (System.currentTimeMillis() - start < GIVE_UP_THUMB)
                    {
                        try
                        {
                            Thread.sleep(250);
                        }
                        catch (InterruptedException e)
                        {
                        }

                        if (mMediaPlayer.getCurrentPosition() > 0)
                        {
                            mMediaPlayer.pause();
                            break;
                        }
                    }
                    mMediaPlayer.setVolume(1f, 1f);
                    initReset();
                }
            });

            mMediaPlayer.setOnBufferingUpdateListener(this);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void initReset()
    {
        initComplete = true;

        if (mMediaPlayer.getCurrentPosition() == 0)
        {
            initDone();
        }
        else
        {
            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener()
            {
                @Override
                public void onSeekComplete(MediaPlayer mp)
                {
                    mMediaPlayer.setOnSeekCompleteListener(null);
                    initDone();
                }
            });

            mMediaPlayer.seekTo(0);
        }
    }

    private void initDone()
    {
        setVisibility(View.VISIBLE);
        if (startCalled)
        {
            mMediaPlayer.start();
            startCalled = false;
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        return false;
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
