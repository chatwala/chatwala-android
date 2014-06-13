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
 * Date: 6/2/2014
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserSentboxRequest extends CwHttpRequest {
    public GetUserSentboxRequest() throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "messages/userOutbox", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("user_id", UserManager.getUserId());

        setBody(new JSONObjectBody(json));
    }
}
