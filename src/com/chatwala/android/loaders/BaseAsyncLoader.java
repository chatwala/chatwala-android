package com.chatwala.android.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import com.crashlytics.android.Crashlytics;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 5:35 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseAsyncLoader<D> extends AsyncTaskLoader<D>
{
    private D data;
    private LoaderBroadcastReceiver loaderBroadcastReceiver = null;

    public BaseAsyncLoader(Context context)
    {
        super(context);
    }

    @Override
    protected void onStartLoading()
    {
        if (data != null)
        {
            deliverResult(data);
        }

        if(loaderBroadcastReceiver == null)
        {
            loaderBroadcastReceiver = new LoaderBroadcastReceiver(this);
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(loaderBroadcastReceiver, new IntentFilter(getBroadcastString()));
        }

        if (takeContentChanged() || data == null)
        {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(D data)
    {
        Crashlytics.log("deliverResult " + this.getClass().getName());
        if (isReset())
        {
            if (data != null)
            {
                onReleaseResources(data);
            }
        }

        D oldData = this.data;
        this.data = data;

        if(isStarted())
        {
            super.deliverResult(data);
        }

        if(oldData != null)
        {
            onReleaseResources(oldData);
        }
    }

    @Override
    protected void onStopLoading()
    {
        Crashlytics.log("onStopLoading " + this.getClass().getName());
        cancelLoad();
    }

    @Override
    protected void onReset()
    {
        Crashlytics.log("onReset " + this.getClass().getName());
        super.onReset();

        onStopLoading();

        if(data != null)
        {
            onReleaseResources(data);
            data = null;
        }

        if(loaderBroadcastReceiver != null)
        {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(loaderBroadcastReceiver);
            loaderBroadcastReceiver = null;
        }
    }

    protected void onReleaseResources(D data){}
    protected abstract String getBroadcastString();
}
