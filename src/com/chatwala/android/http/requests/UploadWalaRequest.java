package com.chatwala.android.http.requests;

import com.chatwala.android.http.BlobRequest;
import com.chatwala.android.http.HttpMethod;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 9:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadWalaRequest extends BlobRequest {
    public UploadWalaRequest(String uri, File wala) throws Exception {
        super(uri, HttpMethod.PUT, wala, "application/octet-stream");
    }
}
