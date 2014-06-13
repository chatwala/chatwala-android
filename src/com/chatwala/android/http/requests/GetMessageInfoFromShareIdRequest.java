package com.chatwala.android.http.requests;

import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;
import com.koushikdutta.async.http.body.JSONObjectBody;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/9/2014
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessageInfoFromShareIdRequest extends CwHttpRequest {
    public GetMessageInfoFromShareIdRequest(String shareId) throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "messages/getReadUrlFromShareId", HttpMethod.POST);

        JSONObject body = new JSONObject();
        body.put("share_id", shareId);

        setBody(new JSONObjectBody(body));
    }
}
