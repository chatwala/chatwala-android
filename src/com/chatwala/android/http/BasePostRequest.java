package com.chatwala.android.http;

import android.content.Context;
import co.touchlab.android.superbus.http.BusHttpClient;
import com.chatwala.android.util.Logger;
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
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BasePostRequest<T> extends BaseHttpRequest<T>
{
    public BasePostRequest(Context context)
    {
        super(context);
    }

    @Override
    protected HttpResponse makeRequest(BusHttpClient client)
    {
        return client.post(getResourceURL(), getContentType(), getPostData());
    }

    protected String getContentType()
    {
        return "application/json";
    }

    private byte[] getPostData()
    {
        try
        {
           String body = makeBodyJson().toString();
           return body.getBytes("utf-8");

        }
        catch (UnsupportedEncodingException e)
        {
            Crashlytics.logException(e);
            throw new RuntimeException("The Post Data encoding is not supported");
        }
        catch (JSONException e)
        {
            Crashlytics.logException(e);
            throw new RuntimeException("There was an error making the JSON for the Post payload");
        }
        catch (SQLException e)
        {
            Crashlytics.logException(e);
            throw new RuntimeException("There was an error retrieving saved jobs for bulk post");
        }
    }

    protected abstract JSONObject makeBodyJson() throws JSONException, SQLException;
}
