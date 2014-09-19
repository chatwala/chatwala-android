package com.chatwala.android.http;

import android.net.Uri;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.FileBody;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/12/2014
 * Time: 5:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class BlobRequest extends AsyncHttpRequest {
    public BlobRequest(String uri, HttpMethod method, File file, String contentType) throws Exception {
        this(Uri.parse(uri), method, file, contentType);
    }

    public BlobRequest(Uri uri, HttpMethod method, File file, final String contentType) throws Exception {
        super(uri, method.toString());

        setHeader("x-ms-blob-type", "BlockBlob");
        setHeader("Content-Length", Long.toString(file.length()));
        setBody(new FileBody(file) {
            @Override
            public String getContentType() {
                return contentType;
            }
        });
    }

    public void log() {
        NetworkLogger.log(this);
    }
}
