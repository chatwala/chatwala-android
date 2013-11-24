package co.touchlab.customcamera;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.VideoView;

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
    private File video;
    private int width;
    private int height;
    private int rotation;
    public MediaPlayer mediaPlayer;

    public DynamicVideoView(final Context context, File video, int width, int height, int rotation)
    {
        super(context);
        this.video = video;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mp)
            {
                DynamicVideoView.this.mediaPlayer = mp;
                int volume = context.getResources().getInteger(R.integer.video_playback_volume);
                mediaPlayer.setVolume((float)volume/100f, (float)volume/100f);
            }
        });
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
        if (((ViewGroup) getParent()).getHeight() != 0)
        {
            //either scale up the height of the preview, so that is at least the size of the container
            int viewHeight = ((ViewGroup) getParent()).getHeight();
            int previewHeight = Math.max(width, height);
            int previewWidth = Math.min(width, height);
            double ratio = (double) viewHeight / (double) previewWidth;

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
