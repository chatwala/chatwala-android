package com.chatwala.android.http.requests;

import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;
import com.chatwala.android.messages.ChatwalaMessageBase;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/13/2014
 * Time: 11:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessageImageRequest extends CwHttpRequest {
    public GetMessageImageRequest(ChatwalaMessageBase message) throws Exception {
        super(message.getImageUrl(), HttpMethod.GET);

        if(message.getImageModifiedSince() != null) {
            setHeader("If-Modified-Since", message.getImageModifiedSince());
        }
    }
}
