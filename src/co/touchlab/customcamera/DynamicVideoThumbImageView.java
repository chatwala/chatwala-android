package co.touchlab.customcamera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/22/13
 * Time: 12:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class DynamicVideoThumbImageView extends ImageView
{
    private int width;
    private int height;

    public DynamicVideoThumbImageView(final Context context, int width, int height)
    {
        super(context);
        this.width = width;
        this.height = height;
    }

    public DynamicVideoThumbImageView(Context context, AttributeSet attrs)
    {
        this();
        throw new UnsupportedOperationException("Can only be set manually");
    }

    public DynamicVideoThumbImageView(Context context, AttributeSet attrs, int defStyle)
    {
        this();
        throw new UnsupportedOperationException("Can only be set manually");
    }

    private DynamicVideoThumbImageView()
    {
        super(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (((ViewGroup) getParent()).getHeight() != 0)
        {
            //either scale up the height of the preview, so that is at least the size of the container
            int viewWidth = ((ViewGroup)getParent()).getWidth();
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
