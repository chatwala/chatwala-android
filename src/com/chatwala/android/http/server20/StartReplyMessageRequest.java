package com.chatwala.android.http.server20;

import android.content.Context;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.http.BasePostRequest;
import com.chatwala.android.util.Logger;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by samirahman on 3/11/14.
 */
public class StartReplyMessageRequest extends BasePostRequest {


    private ChatwalaResponse<ChatwalaMessage> chatwalaResponse=null;
    String messageId;
    String replyingToMessageId;
    double startRecording;

    public StartReplyMessageRequest(Context context, String messageId, String replyingToMessageId, double startRecording) {
        super(context);
        this.messageId= messageId;
        this.replyingToMessageId = replyingToMessageId;
        this.startRecording = startRecording;
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException {

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("user_id", AppPrefs.getInstance(context).getUserId());
        bodyJson.put("message_id", messageId);
        bodyJson.put("replying_to_message_id", replyingToMessageId);
        bodyJson.put("start_recording", startRecording);
        return bodyJson;
        //'{"message_id":"km23", "user_id":"d11111111-11111-1111-1111-111111111111", "replying_to_message_id":"km22", "start_recording": 0}'
    }

    @Override
    protected String getResourceURL() {
        return "messages/startReplyMessageSend";
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
