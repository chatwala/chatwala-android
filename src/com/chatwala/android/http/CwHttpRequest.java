package com.chatwala.android.http;

import android.net.Uri;
import com.chatwala.android.app.ChatwalaApplication;
import com.koushikdutta.async.http.AsyncHttpRequest;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/8/2014
 * Time: 5:47 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CwHttpRequest extends AsyncHttpRequest {
    public CwHttpRequest(String uri, HttpMethod method) throws Exception {
        this(Uri.parse(uri), method);
    }

    public CwHttpRequest(Uri uri, HttpMethod method) throws Exception {
        super(uri, method.toString());

        setHeader("x-chatwala", "58041de0bc854d9eb514d2f22d50ad4c:ac168ea53c514cbab949a80bebe09a8a");
        setHeader("x-chatwala-appversion", ChatwalaApplication.getVersionName());
    }

    public void log() {
        NetworkLogger.log(this);
    }
}
