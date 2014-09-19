package com.chatwala.android.http.requests;

import com.chatwala.android.files.ImageManager;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/13/2014
 * Time: 6:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserImageRequest extends CwHttpRequest {
    public GetUserImageRequest(String userId, String url) throws Exception {
        this(userId, url, null);
    }

    public GetUserImageRequest(String userId, String url, String lastModified) throws Exception {
        super(url, HttpMethod.GET);

        if(lastModified == null) {
            lastModified = ImageManager.getUserImageLastModified(userId);
        }
        if(lastModified != null) {
            setHeader("If-Modified-Since", lastModified);
        }
    }
}
