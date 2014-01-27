package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import com.chatwala.android.AppPrefs;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by matthewdavis on 1/27/14.
 */
public class PostRegisterGCMRequest extends BasePostRequest
{

    public PostRegisterGCMRequest(Context context)
    {
        super(context);

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        try
        {
            String regid = gcm.register(AppPrefs.getInstance(context).getUserId());
            AppPrefs.getInstance(context).setGcmToken(regid);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException
    {
        AppPrefs prefs = AppPrefs.getInstance(context);
        JSONObject object = new JSONObject();
        object.put("user_id", prefs.getUserId());
        object.put("platform_type", "android");
        object.put("push_token", prefs.getGcmToken());
        return object;
    }

    @Override
    protected String getResourceURL()
    {
        return "register";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        Log.d("############ GCM Response: ", response.getBodyAsString());
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;
    }
}
