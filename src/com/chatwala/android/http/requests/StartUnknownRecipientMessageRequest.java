package com.chatwala.android.http.requests;

import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;
import com.chatwala.android.users.UserManager;
import com.koushikdutta.async.http.body.JSONObjectBody;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 6:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class StartUnknownRecipientMessageRequest extends CwHttpRequest {

    public StartUnknownRecipientMessageRequest(String messageId) throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "messages/startUnknownRecipientMessageSend", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("message_id", messageId);
        json.put("sender_id", UserManager.getUserId());

        setBody(new JSONObjectBody(json));
    }
}
