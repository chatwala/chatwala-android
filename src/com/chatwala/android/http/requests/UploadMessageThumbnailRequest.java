package com.chatwala.android.http.requests;

import com.chatwala.android.http.BlobRequest;
import com.chatwala.android.http.HttpMethod;
import com.chatwala.android.messages.ChatwalaMessageBase;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadMessageThumbnailRequest extends BlobRequest {

    public UploadMessageThumbnailRequest(String uri, ChatwalaMessageBase message) throws Exception {
        super(uri, HttpMethod.PUT, message.getLocalMessageThumb(), "image/png");
    }
}
