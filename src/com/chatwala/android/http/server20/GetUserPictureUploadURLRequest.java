package com.chatwala.android.http.server20;

import android.content.Context;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.http.BasePostRequest;
import com.chatwala.android.util.Logger;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;


/**
 * Created by samirahman on 3/11/14.
 */
public class GetUserPictureUploadURLRequest extends BasePostRequest {


    private ChatwalaResponse<String> chatwalaResponse=null;

    public GetUserPictureUploadURLRequest(Context context) {
        super(context);
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException {

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("user_id", AppPrefs.getInstance(context).getUserId());
        return bodyJson;
    }

    @Override
    protected String getResourceURL() {
        return "user/postUserProfilePicture";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException {
        chatwalaResponse = new ChatwalaResponse<String>();

        JSONObject bodyJson = new JSONObject(response.getBodyAsString());
        JSONObject response_code = bodyJson.getJSONObject("response_code");
        String writeUrl = bodyJson.getString("write_url");

        chatwalaResponse.setResponseCode(response_code.getInt("code"));
        chatwalaResponse.setResponseMessage(response_code.getString("message"));

        chatwalaResponse.setResponseData(writeUrl);
    }

    protected ChatwalaResponse<String> getReturnValue()
    {
        return chatwalaResponse;
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;
    }
}
