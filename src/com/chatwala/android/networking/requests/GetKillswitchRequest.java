package com.chatwala.android.networking.requests;

import android.content.Context;
import com.chatwala.android.CWResult;
import com.chatwala.android.EnvironmentVariables;
import com.chatwala.android.networking.NetworkCallable;
import com.chatwala.android.networking.NetworkLogger;
import com.chatwala.android.networking.Request;
import com.chatwala.android.util.KillswitchInfo;
import com.chatwala.android.util.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Eliezer on 4/9/2014.
 */
public class GetKillswitchRequest implements Request<HttpURLConnection, JSONObject> {
    private KillswitchInfo oldKillswitch;

    public GetKillswitchRequest(KillswitchInfo oldKillswitch) {
        this.oldKillswitch = oldKillswitch;
    }

    private String getUrl() {
        return EnvironmentVariables.get().getKillswitchPath();
    }

    @Override
    public NetworkCallable<HttpURLConnection, JSONObject> getCallable(Context context, int numRetries) {
        return new NetworkCallable<HttpURLConnection, JSONObject>(context, this, numRetries);
    }

    @Override
    public HttpURLConnection getConnection(boolean isRetry) throws Exception {
        HttpURLConnection client = (HttpURLConnection) new URL(getUrl()).openConnection();
        client.setDoInput(true);
        return client;
    }

    @Override
    public CWResult<Boolean> makeRequest(HttpURLConnection client) throws Exception {
        if(oldKillswitch.getLastModified() != null) {
            client.setRequestProperty("If-Modified-Since", oldKillswitch.getLastModified());
        }

        NetworkLogger.logHttpRequest(client, getUrl(), "<no_request_body>");

        return new CWResult<Boolean>().setSuccess(true);
    }

    @Override
    public CWResult<JSONObject> parseResponse(HttpURLConnection client) throws Exception {
        StringBuilder responseBuilder = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String line;
            while((line = in.readLine()) != null) {
                responseBuilder.append(line);
            }
            in.close();

            if(client.getResponseCode() == 304) {
                NetworkLogger.logHttpResponse(client, getUrl(), "<no_data>");
                return new CWResult<JSONObject>().setSuccess(oldKillswitch.toJson());
            }

            JSONObject killswitch;
            try {
                killswitch = new JSONObject(responseBuilder.toString());
                killswitch.put(KillswitchInfo.LAST_MODIFIED_KEY, client.getHeaderField("Last-Modified"));
            }
            catch(Exception e) {
                Logger.e("Couldn't parse the killswitch JSON", e);
                killswitch = new JSONObject();

            }

            CWResult<JSONObject> response = new CWResult<JSONObject>().setSuccess(killswitch);
            NetworkLogger.logHttpResponse(client, getUrl(), response);
            return response;
        }
        catch(Exception e) {
            Logger.e("Got an error parsing the response", e);
            throw e;
        }
    }

    @Override
    public void terminateConnection(HttpURLConnection client) {
        client.disconnect();
    }

    @Override
    public String getGenericErrorMessage() {
        return "There was an error getting the killswitch.";
    }
}
