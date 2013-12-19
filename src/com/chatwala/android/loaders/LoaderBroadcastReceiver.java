package com.chatwala.android.loaders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 5:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoaderBroadcastReceiver extends BroadcastReceiver
{
    private BaseAsyncLoader loader;

    public LoaderBroadcastReceiver(BaseAsyncLoader loader)
    {
        this.loader = loader;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        loader.onContentChanged();
    }
}
