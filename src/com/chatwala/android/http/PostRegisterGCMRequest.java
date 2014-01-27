package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import com.chatwala.android.AppPrefs;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by matthewdavis on 1/27/14.
 */
public class PostRegisterGCMRequest extends BasePostRequest
{

    public PostRegisterGCMRequest(Context context)
    {
        super(context);
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
