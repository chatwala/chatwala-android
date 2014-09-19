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
 * Date: 5/27/2014
 * Time: 12:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserProfilePicReadUrlRequest extends CwHttpRequest {
    public GetUserProfilePicReadUrlRequest() throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "user/postGetReadURLForUserProfilePicture", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("user_id", UserManager.getUserId());

        setBody(new JSONObjectBody(json));
    }
}
