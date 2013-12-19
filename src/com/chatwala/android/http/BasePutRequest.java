package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.http.BusHttpClient;
import com.crashlytics.android.Crashlytics;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BasePutRequest extends BaseHttpRequest
{
    public BasePutRequest(Context context)
    {
        super(context);
    }

    @Override
    protected HttpResponse makeRequest(BusHttpClient client)
    {
        Crashlytics.log("makeRequest " + this.getClass().getName());
        //return client.post(getResourceURL(), getContentType(), getPutData());
        return client.put(getResourceURL(), getContentType(), getPutData());
    }

    protected String getContentType()
    {
        return "video/mp4";
    }

    protected abstract byte[] getPutData();

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        Log.d("##########", response.getBodyAsString());
        // No response
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
