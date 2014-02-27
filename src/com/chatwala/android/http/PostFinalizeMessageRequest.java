package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.util.Logger;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by matthewdavis on 1/31/14.
 */
public class PostFinalizeMessageRequest extends BasePostRequest
{
    String messageId, senderId, receiverId;

    public PostFinalizeMessageRequest(Context context, String messageId, String senderId, String receiverId)
    {
        super(context);
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException
    {
        JSONObject toReturn = new JSONObject();
        toReturn.put("sender_id", senderId);
        toReturn.put("recipient_id", receiverId);
        return toReturn;
    }

    @Override
    protected String getResourceURL()
    {
        return "messages/" + messageId + "/finalize";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException
    {
        Logger.i("Message finalize resposne is - " + response.getBodyAsString());
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;
    }
}
