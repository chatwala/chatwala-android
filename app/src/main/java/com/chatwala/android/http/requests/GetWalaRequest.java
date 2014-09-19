package com.chatwala.android.http.requests;

import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;
import com.chatwala.android.messages.ChatwalaMessageBase;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 12:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetWalaRequest<T extends ChatwalaMessageBase> extends CwHttpRequest {

    public GetWalaRequest(T message) throws Exception {
        super(message.getReadUrl(), HttpMethod.GET);
    }
}
