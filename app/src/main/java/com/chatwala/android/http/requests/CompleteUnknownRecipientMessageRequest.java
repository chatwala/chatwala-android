package com.chatwala.android.http.requests;

import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;
import com.chatwala.android.messages.ChatwalaSentMessage;
import com.koushikdutta.async.http.body.JSONObjectBody;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 9:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class CompleteUnknownRecipientMessageRequest extends CwHttpRequest {

    public CompleteUnknownRecipientMessageRequest(ChatwalaSentMessage message) throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "messages/completeUnknownRecipientMessageSend", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("message_id", message.getMessageId());

        setBody(new JSONObjectBody(json));
    }
}
