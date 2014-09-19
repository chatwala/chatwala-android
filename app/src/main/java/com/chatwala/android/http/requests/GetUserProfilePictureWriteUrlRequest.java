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
 * Date: 5/11/2014
 * Time: 7:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserProfilePictureWriteUrlRequest extends CwHttpRequest {

    public GetUserProfilePictureWriteUrlRequest() throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "user/postUserProfilePicture", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("user_id", UserManager.getUserId());

        setBody(new JSONObjectBody(json));
    }
}
