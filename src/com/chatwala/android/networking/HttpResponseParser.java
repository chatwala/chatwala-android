package com.chatwala.android.networking;

import java.net.HttpURLConnection;

/**
 * Created by samirahman on 3/20/14.
 */
public interface HttpResponseParser {

    public HttpResponseResult parse(HttpURLConnection connection);
}
