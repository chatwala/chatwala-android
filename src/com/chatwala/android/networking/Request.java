package com.chatwala.android.networking;

import com.chatwala.android.CWResult;

/**
 * Created by Eliezer on 3/24/2014.
 */
public interface Request<TClient, TResponse> {
    public TClient getConnection() throws Exception;

    public CWResult<Boolean> makeRequest(TClient client) throws Exception;

    public CWResult<TResponse> parseResponse(TClient client) throws Exception;

    public void terminateConnection(TClient client);

    public String getGenericErrorMessage();
}
