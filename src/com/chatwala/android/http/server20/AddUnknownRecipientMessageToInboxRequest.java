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
public class AddUnknownRecipientMessageToInboxRequest extends BasePostRequest {

    String message_id;

    public AddUnknownRecipientMessageToInboxRequest(Context context, String message_id) {
        super(context);

        this.message_id= message_id;
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException {
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("recipient_id", AppPrefs.getInstance(context).getUserId());
        bodyJson.put("message_id", message_id);
        return bodyJson;

    }

    @Override
    protected String getResourceURL() {
        return "messages/addUnknownRecipientMessageToInbox";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException {
        Logger.i("AddUnknownRecipientMessageToInboxRequest response is - " + response.getBodyAsString());
    }

    @Override
    protected boolean hasDbOperation() {
        return false;
    }
}
