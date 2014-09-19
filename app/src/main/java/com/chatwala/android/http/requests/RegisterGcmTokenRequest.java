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
 * Date: 5/19/2014
 * Time: 6:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class RegisterGcmTokenRequest extends CwHttpRequest {

    public RegisterGcmTokenRequest(String gcmToken) throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "user/registerPushToken", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("user_id", UserManager.getUserId());
        json.put("platform_type", "android");
        json.put("push_token", gcmToken);

        setBody(new JSONObjectBody(json));
    }
}
