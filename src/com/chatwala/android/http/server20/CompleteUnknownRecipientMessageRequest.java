package com.chatwala.android.http.server20;

import android.content.Context;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.http.BasePostRequest;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by samirahman on 3/11/14.
 */
public class CompleteUnknownRecipientMessageRequest extends BasePostRequest {

    private ChatwalaResponse<ChatwalaMessage> chatwalaResponse=null;
    String message_id;

    public CompleteUnknownRecipientMessageRequest(Context context, String message_id) {
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
        return "messages/completeUnknownRecipientMessageSend";
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
        currentMessage.setMessageId(message_meta_data.getString("message_id"));
        currentMessage.setRecipientId(message_meta_data.getString("recipient_id"));
        currentMessage.setSenderId(message_meta_data.getString("sender_id"));
        currentMessage.setThumbnailUrl(message_meta_data.getString("thumbnail_url"));
        currentMessage.setUrl(message_meta_data.getString("read_url"));
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
