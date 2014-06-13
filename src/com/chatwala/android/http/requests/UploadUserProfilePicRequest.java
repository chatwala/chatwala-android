package com.chatwala.android.http.requests;

import com.chatwala.android.files.FileManager;
import com.chatwala.android.http.BlobRequest;
import com.chatwala.android.http.HttpMethod;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 7:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadUserProfilePicRequest extends BlobRequest {
    public UploadUserProfilePicRequest(String uri) throws Exception {
        super(uri, HttpMethod.PUT, FileManager.getUserProfilePic(), "image/png");
    }
}
