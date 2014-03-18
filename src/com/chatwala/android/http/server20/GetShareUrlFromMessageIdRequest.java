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
public class GetShareUrlFromMessageIdRequest extends BasePostRequest {

    String messageId;
    private ChatwalaResponse<String> chatwalaResponse=null;

    public GetShareUrlFromMessageIdRequest(Context context, String messageId) {
        super(context);
        this.messageId=messageId;
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException {

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("message_id", messageId);
        return bodyJson;
    }

    @Override
    protected String getResourceURL() {
        return "messages/getShareUrlFromMessageId";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException {
        chatwalaResponse = new ChatwalaResponse<String>();

        JSONObject bodyJson = new JSONObject(response.getBodyAsString());

        JSONObject response_code = bodyJson.getJSONObject("response_code");
        String share_url = bodyJson.getString("share_url");

        chatwalaResponse.setResponseData(share_url);
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
