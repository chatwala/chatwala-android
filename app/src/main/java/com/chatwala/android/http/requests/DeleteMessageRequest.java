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
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 1:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class DeleteMessageRequest extends CwHttpRequest {
    public DeleteMessageRequest(ChatwalaMessage message) throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "messages/markMessageAsDeleted", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("message_id", message.getMessageId());
        json.put("user_id", UserManager.getUserId());

        setBody(new JSONObjectBody(json));
    }
}
