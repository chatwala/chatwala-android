package com.chatwala.android.http.server20;

import android.content.Context;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.http.BasePostRequest;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by samirahman on 3/11/14.
 */
public class RenewWriteUrlForMessageRequest extends BasePostRequest {


    private ChatwalaResponse<String> chatwalaResponse=null;
    String messageId;
    String shardKey;

    public RenewWriteUrlForMessageRequest(Context context, String messageId, String shardKey) {
        super(context);
        this.messageId= messageId;
        this.shardKey = shardKey;

    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException {

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("message_id", messageId);
        bodyJson.put("shard_key", shardKey);
        return bodyJson;
    }

    @Override
    protected String getResourceURL() {
        return "messages/renewWriteUrlForMessage";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException {
        chatwalaResponse = new ChatwalaResponse<String>();

        JSONObject bodyJson = new JSONObject(response.getBodyAsString());
        JSONObject response_code = bodyJson.getJSONObject("response_code");

        chatwalaResponse.setResponseCode(response_code.getInt("code"));
        chatwalaResponse.setResponseMessage(response_code.getString("message"));
        chatwalaResponse.setResponseData(bodyJson.getString("write_url"));

    }

    protected ChatwalaResponse<String> getReturnValue()
    {
        return chatwalaResponse;
    }

    @Override
    protected boolean hasDbOperation() {
        return false;
    }
}
