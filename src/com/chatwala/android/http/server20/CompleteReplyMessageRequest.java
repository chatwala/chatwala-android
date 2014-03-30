package com.chatwala.android.http.server20;

import android.content.Context;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.http.BasePostRequest;
import com.chatwala.android.util.Logger;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by samirahman on 3/11/14.
 */
public class CompleteReplyMessageRequest  extends BasePostRequest {

    private ChatwalaResponse<ChatwalaMessage> chatwalaResponse=null;
    String message_id;

    public CompleteReplyMessageRequest(Context context, String message_id) {
        super(context);
        this.message_id= message_id;
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException {
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("message_id", message_id);
        return bodyJson;
    }

    @Override
    protected String getResourceURL() {
        return "messages/completeReplyMessageSend";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException {
        Logger.d("Not much to do here!");
        chatwalaResponse = new ChatwalaResponse<ChatwalaMessage>();

        JSONObject bodyJson = new JSONObject(response.getBodyAsString());
        JSONObject response_code = bodyJson.getJSONObject("response_code");

        chatwalaResponse.setResponseCode(response_code.getInt("code"));
        chatwalaResponse.setResponseMessage(response_code.getString("message"));
    }

    protected ChatwalaResponse<ChatwalaMessage> getReturnValue()
    {

        return chatwalaResponse;
    }


    @Override
    protected boolean hasDbOperation() {
        return false;
    }
}
