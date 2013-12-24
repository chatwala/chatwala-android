package com.chatwala.android;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.VideoView;
import com.chatwala.android.util.AndroidUtils;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/22/13
 * Time: 12:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class DynamicVideoView extends VideoView
{
    private final int VIDEO_DISPLAY_DELAY = 100;
    public static final int GIVE_UP_THUMB = 5000;
    private File video;
    private int width;
    private int height;
    public MediaPlayer mediaPlayer;
    private Handler handler;
    VideoReadyCallback callback;

    public interface VideoReadyCallback
    {
        void ready();
    }

    public DynamicVideoView(final Context context, File video, int width, int height, VideoReadyCallback callback, final boolean runStart)
    {
        super(context);
        this.callback = callback;
        this.video = video;
        setVideoPath(video.getPath());
        this.width = width;
        this.height = height;
        handler = new Handler();
        setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mp)
            {
                DynamicVideoView.this.mediaPlayer = mp;
//                resetVolume(context);
                if(runStart)
                    runningStart();
            }
        });
//        runningStart();
    }

    private void resetVolume(Context context)
    {
        int volume = context.getResources().getInteger(R.integer.video_playback_volume);
        mediaPlayer.setVolume((float) volume / 100f, (float) volume / 100f);
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

        mediaPlayer.setVolume(0f, 0f);
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

                    if (getCurrentPosition() > 0)
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
                        mediaPlayer.seekTo(0);
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
}
