package com.chatwala.android.networking;

import android.content.Context;
import com.chatwala.android.CWResult;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.NetworkUtils;

import java.util.concurrent.Callable;

/**
 * Created by Eliezer on 3/24/2014.
 */
public class NetworkCallable<TClient, TResponse> implements Callable<CWResult<TResponse>> {
    private final Context context;
    private final Request<TClient, TResponse> request;
    private final int numRetries;

    public NetworkCallable(Context context, Request<TClient, TResponse> request, int numRetries) {
        this.context = context;
        this.request = request;
        this.numRetries = numRetries;
    }

    @Override
    public CWResult<TResponse> call() throws Exception {
        if(!NetworkUtils.isConnectedToInternet(context)) {
            return new CWResult<TResponse>().setError("There is no connection to the internet.");
        }

        TClient client = null;
        for(int i = 0; i <= numRetries; i++) {
            try {
                client = request.getConnection(i > 0);
                if(client == null) {
                    throw new NullPointerException("The request client cannot be null");
                }
                CWResult<Boolean> requestResult = request.makeRequest(client);
                if(!requestResult.isSuccess()) {
                    if(i < numRetries - 1) {
                        continue;
                    }
                    else {
                        return new CWResult<TResponse>(requestResult);
                    }
                }
                return request.parseResponse(client);
            }
            catch(Exception e) {
                Logger.e("Got an exception while making a network request", e);
                if(i < numRetries - 1) {
                    continue;
                }
                else {
                    return new CWResult<TResponse>().setError(request.getGenericErrorMessage());
                }
            }
            finally {
                if(client != null) {
                    request.terminateConnection(client);
                }
            }
        }

        return new CWResult<TResponse>().setError(request.getGenericErrorMessage());
    }
}
