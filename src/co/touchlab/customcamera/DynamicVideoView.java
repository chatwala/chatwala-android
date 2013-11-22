package co.touchlab.customcamera;

import android.content.Context;
import android.hardware.Camera;
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

    public DynamicVideoView(Context context, File video, int width, int height)
    {
        super(context);
        this.video = video;
        this.width = width;
        this.height = height;
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
            int previewHeight = width;
            int previewWidth = height;
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
