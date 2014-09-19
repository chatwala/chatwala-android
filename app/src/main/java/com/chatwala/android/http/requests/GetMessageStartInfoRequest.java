package com.chatwala.android.http.requests;

import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;
import com.koushikdutta.async.http.body.JSONObjectBody;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 5:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessageStartInfoRequest extends CwHttpRequest {

    public GetMessageStartInfoRequest(String messageId) throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "messages/getShortUrlFromMessageId", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("message_id", messageId);

        setBody(new JSONObjectBody(json));
    }
}
