package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import com.chatwala.android.util.SharedPrefsUtils;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 1:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetRegisterUserRequest extends BaseGetRequest
{
    public GetRegisterUserRequest(Context context)
    {
        super(context);
    }

    @Override
    protected String getResourceURL()
    {
        return "register";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        JSONArray bodyAsJson = new JSONArray(response.getBodyAsString());
        String userId = bodyAsJson.getJSONObject(0).getString("user_id");

//        JSONObject bodyAsJson = new JSONObject(response.getBodyAsString());
//        String userId = bodyAsJson.getString("user_id");

        Log.d("##### USERID", userId);
        SharedPrefsUtils.setUserId(context, userId);
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;
    }
}