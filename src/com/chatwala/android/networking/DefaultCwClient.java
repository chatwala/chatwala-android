package com.chatwala.android.networking;

import android.content.Context;
import com.chatwala.android.CwResult;
import com.chatwala.android.util.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class DefaultCwClient<TResponse> extends Client<HttpURLConnection, TResponse> {

    public DefaultCwClient(Context context, String url) {
        super(context, url);
    }

    @Override
    protected CwResult<HttpURLConnection> initClient() {
        CwResult<HttpURLConnection> result = new CwResult<HttpURLConnection>();
        try {
            result.setSuccess((HttpURLConnection) new URL(getUrl()).openConnection());
        }
        catch(IOException e) {
            Logger.e("There was an error creating the connection for " + getUrl(), e);
            result.setError("There was a problem reaching the server");
        }
        finally {
            return result;
        }
    }

    @Override
    protected final void setKeyAndVersionHeaders(HttpURLConnection client) {
        client.addRequestProperty(getXChatwalaKey(), getXChatwala());
        client.addRequestProperty(getAppVersionKey(), getAppVersion());
    }

    @Override
    protected void logRequest(HttpURLConnection client) {
        Logger.i("==================HTTP REQUEST==================");
        Logger.network("Request to: " + getUrl() +
                        "\n" + client.getRequestProperties().toString().replaceAll("],", "]\n"));
    }

    @Override
    protected void logResponse(HttpURLConnection client, CwResult<Response<TResponse>> response) {
        Logger.i("==================HTTP RESPONSE==================");
        StringBuilder sb = new StringBuilder()
                .append("Response from: ").append(getUrl())
                .append("\n").append(client.getHeaderFields().toString().replaceAll("],", "]\n"));
        if(client.getContentType().contains("application/json") && response.getResult() != null &&
                response.getResult().getData() != null) {
            sb.append("\n").append(response.getResult().getData().toString());
        }
        Logger.i(sb.toString());
    }


}
