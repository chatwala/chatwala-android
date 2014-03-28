package com.chatwala.android.http.server20;

import android.content.Context;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.http.BasePostRequest;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by samirahman on 3/11/14.
 */
public class StartUnknownRecipientMessageRequest extends BasePostRequest {
    String message_id;

    private ChatwalaResponse<ChatwalaMessage> chatwalaResponse;

    public StartUnknownRecipientMessageRequest(Context context, String message_id) {
        super(context);
        this.message_id=message_id;
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException {
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("message_id", message_id);
        bodyJson.put("sender_id", AppPrefs.getInstance(context).getUserId());
        return bodyJson;
    }

    @Override
    protected String getResourceURL() {

        return "messages/startUnknownRecipientMessageSend";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException {

        chatwalaResponse = new ChatwalaResponse<ChatwalaMessage>();

        JSONObject bodyJson = new JSONObject(response.getBodyAsString());
        JSONObject message_meta_data = bodyJson.getJSONObject("message_meta_data");
        JSONObject response_code = bodyJson.getJSONObject("response_code");

        chatwalaResponse.setResponseCode(response_code.getInt("code"));
        chatwalaResponse.setResponseMessage(response_code.getString("message"));

        ChatwalaMessage currentMessage = new ChatwalaMessage();
        currentMessage.populateFromMetaDataJSON(message_meta_data);
        currentMessage.setWriteUrl(bodyJson.getString("write_url"));
        currentMessage.setThumbnailWriteUrl(bodyJson.getString("message_thumbnail_write_url"));
        currentMessage.setMessageMetaDataString(message_meta_data.toString(4));

        chatwalaResponse.setResponseData(currentMessage);
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
