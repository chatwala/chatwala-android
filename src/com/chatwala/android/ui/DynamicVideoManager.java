package com.chatwala.android.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import com.chatwala.android.DynamicVideoThumbImageView;
import com.chatwala.android.DynamicVideoView;

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
        videoView = new DynamicVideoView(context, video, width, height, null);
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
