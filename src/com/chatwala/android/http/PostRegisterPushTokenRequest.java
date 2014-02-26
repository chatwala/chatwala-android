package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.util.GCMUtils;
import com.chatwala.android.util.Logger;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by matthewdavis on 1/30/14.
 */
public class PostRegisterPushTokenRequest extends BasePostRequest
{
    String regid;

    public PostRegisterPushTokenRequest(Context context)
    {
        super(context);

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        try
        {
            regid = gcm.register("419895337876");
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
        object.put("push_token", regid);
        return object;
    }

    @Override
    protected String getResourceURL()
    {
        return "registerPushToken";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        Logger.i("GCM response is " + response.getBodyAsString());

        AppPrefs prefs = AppPrefs.getInstance(context);
        prefs.setGcmToken(regid);

        try
        {
            prefs.setGcmAppVersion(GCMUtils.getAppVersion(context));
        }
        catch (Exception e)
        {
            prefs.setGcmAppVersion(0);
        }
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;
    }
}
