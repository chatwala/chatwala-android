package com.chatwala.android.http;

import android.content.Context;
import co.touchlab.android.superbus.http.BusHttpClient;
import com.turbomanage.httpclient.HttpResponse;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseGetRequest<T> extends BaseHttpRequest<T>
{
    public BaseGetRequest(Context context)
    {
        super(context);
    }

    @Override
    protected HttpResponse makeRequest(BusHttpClient client)
    {
        return client.get(getResourceURL(), null);
    }
}
