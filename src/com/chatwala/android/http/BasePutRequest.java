package com.chatwala.android.http;

import android.content.Context;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.http.BusHttpClient;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;

import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BasePutRequest<T> extends BaseHttpRequest<T>
{
    public BasePutRequest(Context context)
    {
        super(context);
    }

    @Override
    protected HttpResponse makeRequest(BusHttpClient client) throws PermanentException, TransientException
    {
        //return client.post(getResourceURL(), getContentType(), getPutData());
        return client.put(getResourceURL(), getContentType(), getPutData());
    }

    abstract String getContentType();

    protected abstract byte[] getPutData() throws PermanentException, TransientException;

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        // No response
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
