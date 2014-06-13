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
 * Time: 8:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class RenewWriteUrlForMessageRequest extends CwHttpRequest {

    public RenewWriteUrlForMessageRequest(ChatwalaSentMessage message) throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "messages/renewWriteUrlForMessage", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("message_id", message.getMessageId());
        json.put("shard_key", message.getShardKey());

        setBody(new JSONObjectBody(json));
    }
}
