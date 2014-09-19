package com.chatwala.android.http;

import com.koushikdutta.async.http.AsyncHttpResponse;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/9/2014
 * Time: 4:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class CwHttpResponse<T> {
    private AsyncHttpResponse response;
    private T data;

    public CwHttpResponse(AsyncHttpResponse response) throws NullPointerException {
        this(response, null);
    }

    public CwHttpResponse(AsyncHttpResponse response, T data) throws NullPointerException {
        if(response == null) {
            throw new NullPointerException("The underlying HTTP response cannot be null");
        }
        this.response = response;
        this.data = data;
    }

    public T getData() {
        return data;
    }

    /*package*/ void setData(T data) {
        this.data = data;
    }

    public String getUrl() {
        return response.getHeaders().getUri().toString();
    }

    public String getResponseHeadersString() {
        return response.getHeaders().getHeaders().toHeaderString();
    }

    public int getResponseCode() {
        return response.getHeaders().getHeaders().getResponseCode();
    }

    public String getResponseMessage() {
        return response.getHeaders().getHeaders().getResponseMessage();
    }

    public long getContentLength() {
        return response.getHeaders().getContentLength();
    }

    public String getContentType() {
        return response.getHeaders().getHeaders().get("Content-Type");
    }

    public String getLastModified() {
        return response.getHeaders().getHeaders().get("Last-Modified");
    }
}
