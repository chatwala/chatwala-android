package com.chatwala.android.http;

import android.content.Context;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.util.Logger;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by Eliezer on 3/17/14.
 */
public class PostAddToInboxRequest extends BasePostRequest {
    String messageId, userId;

    public PostAddToInboxRequest(Context context, String messageId, String userId)
    {
        super(context);
        this.messageId = messageId;
        this.userId = userId;
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException
    {
        JSONObject toReturn = new JSONObject();
        toReturn.put("message_id", messageId);
        toReturn.put("recipient_id", userId);
        return toReturn;
    }

    @Override
    protected String getResourceURL()
    {
        return "messages/addUnknownRecipientMessageToInbox";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException
    {
        Logger.i("Add unknown recipient message to inbox resposne is - " + response.getBodyAsString());
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;
    }
}
