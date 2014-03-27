package com.chatwala.android.networking.requests;

import android.content.Context;
import com.chatwala.android.CWResult;
import com.chatwala.android.EnvironmentVariables;
import com.chatwala.android.networking.NetworkCallable;
import com.chatwala.android.networking.NetworkLogger;
import com.chatwala.android.networking.NetworkManager;
import com.chatwala.android.networking.Request;
import com.chatwala.android.util.Logger;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Eliezer on 3/24/2014.
 */
public class UserPictureUploadUrlRequest implements Request<HttpURLConnection, JSONObject> {
    private String userId;

    public UserPictureUploadUrlRequest(String userId) {
        this.userId = userId;
    }

    private String getUrl() {
        return EnvironmentVariables.get().getApiPath() + "user/postUserProfilePicture";
    }

    @Override
    public NetworkCallable<HttpURLConnection, JSONObject> getCallable(Context context, int numRetries) {
        return new NetworkCallable<HttpURLConnection, JSONObject>(context, this, numRetries);
    }

    @Override
    public HttpURLConnection getConnection() throws Exception {
        HttpURLConnection client = (HttpURLConnection) new URL(getUrl()).openConnection();
        client.setDoInput(true);
        client.setDoOutput(true); //sets method to POST
        return client;
    }

    @Override
    public CWResult<Boolean> makeRequest(HttpURLConnection client) throws Exception {
        CWResult<Boolean> result = new CWResult<Boolean>();
        JSONObject request = new JSONObject();
        request.put("user_id", userId);

        client.setFixedLengthStreamingMode(request.toString().getBytes().length);
        client.setRequestProperty("Content-Type", "application/json");
        NetworkManager.getInstance().setChatwalaHeaders(client);

        NetworkLogger.logHttpRequest(client, getUrl());

        try {
            PrintWriter out = new PrintWriter(new BufferedOutputStream(client.getOutputStream()));
            out.write(request.toString());
            out.flush();
            out.close();
        }
        catch(Exception e) {
            Logger.e("Got an error sending the request", e);
            throw e;
        }

        return result.setSuccess(true);
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

            CWResult<JSONObject> response = new CWResult<JSONObject>().setSuccess(new JSONObject(responseBuilder.toString()));
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
        return "There was an error uploading the profile picture.";
    }
}
