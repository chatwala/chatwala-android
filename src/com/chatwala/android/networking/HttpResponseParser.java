package com.chatwala.android.networking;

import com.chatwala.android.CwResult;

import java.net.HttpURLConnection;

/**
 * Created by samirahman on 3/20/14.
 */
public interface HttpResponseParser<T> {

    public CwResult<T> parse(HttpURLConnection connection);
}
