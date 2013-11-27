package co.touchlab.customcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;
import co.touchlab.customcamera.util.VideoUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/22/13
 * Time: 12:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class DynamicVideoView extends FrameLayout
{
    private File video;
    private int width;
    private int height;
    private int rotation;
    public MediaPlayer mediaPlayer;
    private InnerVideoView videoView;
    private ImageView thumbFrame;

    public DynamicVideoView(final Context context, File video, int width, int height, int rotation)
    {
        super(context);
        this.video = video;
        this.width = width;
        this.height = height;
        this.rotation = rotation;

        setBackgroundColor(Color.BLUE);

        videoView = new InnerVideoView(context);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        videoView.setLayoutParams(params);
        videoView.setVideoPath(video.getPath());
        addView(videoView);
        videoView.setVisibility(View.GONE);
        thumbFrame = new ImageView(context);
        thumbFrame.setLayoutParams(params);
        addView(thumbFrame);
        new AsyncTask<File, Void, Bitmap>()
        {
            @Override
            protected Bitmap doInBackground(File... params)
            {
                Bitmap videoFrame = VideoUtils.createVideoFrame(params[0].getPath(), 0);
                try
                {
                    File file = new File(Environment.getExternalStorageDirectory(), "thumb_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream stream = new FileOutputStream(file);
                    videoFrame.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                    stream.close();
                }
                catch (IOException e)
                {
                    //
                }
                return videoFrame;
            }

            @Override
            protected void onPostExecute(Bitmap o)
            {

                thumbFrame.setImageBitmap(o);
            }
        }.execute(video);
    }

    public DynamicVideoView(Context context, AttributeSet attrs)
    {
        this();
        throw new UnsupportedOperationException("Can only be set manually");
    }

    public DynamicVideoView(Context context, AttributeSet attrs, int defStyle)
    {
        this();
        throw new UnsupportedOperationException("Can only be set manually");
    }

    private DynamicVideoView()
    {
        super(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // Find out how big everyone wants to be


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
            super.onMeasure((int) newPreviewWidth, (int) newPreviewHeight);
        }
        else
        {
            //The surface needs a non-zero size for the callbacks to trigger
            setMeasuredDimension(1, 1);
        }

//        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    public void start()
    {
        /*thumbFrame.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
        videoView.start();*/
    }

    public boolean isPlaying()
    {
        return videoView.isPlaying();
    }

    public void pause()
    {
        videoView.pause();
    }

    public class InnerVideoView extends VideoView
    {

        public InnerVideoView(Context context)
        {
            super(context);
            setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {
                @Override
                public void onPrepared(MediaPlayer mp)
                {
                    DynamicVideoView.this.mediaPlayer = mp;
                    int volume = getContext().getResources().getInteger(R.integer.video_playback_volume);
                    mediaPlayer.setVolume((float) volume / 100f, (float) volume / 100f);
                }
            });
        }

        public InnerVideoView(Context context, AttributeSet attrs)
        {
            super(context, attrs);
        }

        public InnerVideoView(Context context, AttributeSet attrs, int defStyle)
        {
            super(context, attrs, defStyle);
        }
    }
}
