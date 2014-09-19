package com.chatwala.android.http.requests;

import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.users.UserManager;
import com.koushikdutta.async.http.body.JSONObjectBody;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/10/2014
 * Time: 11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddUnknownRecipientMessageToInboxRequest extends CwHttpRequest {

    public AddUnknownRecipientMessageToInboxRequest(ChatwalaMessage message) throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "messages/addUnknownRecipientMessageToInbox", HttpMethod.POST);

        JSONObject body = new JSONObject();
        body.put("message_id", message.getMessageId());
        body.put("recipient_id", UserManager.getUserId());

        setBody(new JSONObjectBody(body));
    }
}
