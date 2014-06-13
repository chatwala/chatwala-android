package com.chatwala.android.http.requests;

import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;
import com.chatwala.android.users.UserManager;
import com.koushikdutta.async.http.body.JSONObjectBody;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/28/2014
 * Time: 11:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class StartKnownRecipientMessageRequest extends CwHttpRequest {

    public StartKnownRecipientMessageRequest(String messageId, String recipient) throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "messages/startKnownRecipientMessageSend", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("message_id", messageId);
        json.put("sender_id", UserManager.getUserId());
        json.put("recipient_id", recipient);

        setBody(new JSONObjectBody(json));
    }
}
