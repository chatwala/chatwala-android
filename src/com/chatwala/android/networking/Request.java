package com.chatwala.android.networking;

import android.content.Context;
import com.chatwala.android.CWResult;

/**
 * Created by Eliezer on 3/24/2014.
 */
public interface Request<TClient, TResponse> {
    public NetworkCallable<TClient, TResponse> getCallable(Context context, int numRetries);

    public TClient getConnection(boolean isRetry) throws Exception;

    public CWResult<Boolean> makeRequest(TClient client) throws Exception;

    public CWResult<TResponse> parseResponse(TClient client) throws Exception;

    public void terminateConnection(TClient client);

    public String getGenericErrorMessage();
}
