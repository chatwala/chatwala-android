package co.touchlab.customcamera.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import co.touchlab.customcamera.DynamicVideoThumbImageView;
import co.touchlab.customcamera.DynamicVideoView;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/27/13
 * Time: 1:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class DynamicVideoManager
{
    private DynamicVideoThumbImageView imageView;
    private DynamicVideoView videoView;
    private ViewGroup container;

    public DynamicVideoManager(final Context context, ViewGroup container, File video, int width, int height)
    {

        imageView = new DynamicVideoThumbImageView(context, width, height);
        videoView = new DynamicVideoView(context, video, width, height);
        this.container = container;
    }

    public void start()
    {
        removeViewFromContainer(imageView);
        addViewToContainer(videoView);
        videoView.start();
    }

    public void removeAll()
    {
        removeViewFromContainer(imageView);
        removeViewFromContainer(videoView);
    }


    private void addViewToContainer(View view)
    {
        if(container.indexOfChild(view) < 0)
            container.addView(view);
    }

    private void removeViewFromContainer(View view)
    {
        if(container.indexOfChild(view) < 0)
            container.addView(view);
    }
}
