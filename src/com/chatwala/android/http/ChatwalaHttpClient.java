package com.chatwala.android.http;

import co.touchlab.android.superbus.http.BusHttpClient;
import com.turbomanage.httpclient.HttpMethod;
import com.turbomanage.httpclient.multipart.MultipartWrapper;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 1/20/14
 * Time: 1:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaHttpClient extends BusHttpClient
{
    public ChatwalaHttpClient(String baseUrl)
    {
        super(baseUrl);
    }

    @Override
    protected void prepareConnection(HttpURLConnection urlConnection, HttpMethod httpMethod, String contentType, MultipartWrapper multipartWrapper) throws IOException
    {
        super.prepareConnection(urlConnection, httpMethod, contentType, multipartWrapper);
        urlConnection.setInstanceFollowRedirects(true);
    }
}
