package com.chatwala.android.networking.requests;

import android.content.Context;
import com.chatwala.android.CWResult;
import com.chatwala.android.EnvironmentVariables;
import com.chatwala.android.database.ChatwalaMessage;
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
 * Created by Eliezer on 4/10/2014.
 */
public class DeleteMessageRequest implements Request<HttpURLConnection, Boolean> {
    private ChatwalaMessage message;

    public DeleteMessageRequest(ChatwalaMessage message) {
        this.message = message;
    }

    private String getUrl() {
        return EnvironmentVariables.get().getApiPath() + "messages/markMessageAsDeleted";
    }

    @Override
    public NetworkCallable<HttpURLConnection, Boolean> getCallable(Context context, int numRetries) {
        return new NetworkCallable<HttpURLConnection, Boolean>(context, this, numRetries);
    }

    @Override
    public HttpURLConnection getConnection(boolean isRetry) throws Exception {
        HttpURLConnection client = (HttpURLConnection) new URL(getUrl()).openConnection();
        client.setDoInput(true);
        client.setDoOutput(true); //sets method to POST
        return client;
    }

    @Override
    public CWResult<Boolean> makeRequest(HttpURLConnection client) throws Exception {
        CWResult<Boolean> result = new CWResult<Boolean>();
        JSONObject request = new JSONObject();
        request.put("message_id", message.getMessageId());
        request.put("user_id", message.getRecipientId());

        client.setFixedLengthStreamingMode(request.toString().getBytes().length);
        client.setRequestProperty("Content-Type", "application/json");
        NetworkManager.getInstance().setChatwalaHeaders(client);

        NetworkLogger.logHttpRequest(client, getUrl(), request.toString());

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
    public CWResult<Boolean> parseResponse(HttpURLConnection client) throws Exception {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            while(in.readLine() != null) {}
            in.close();

            CWResult<Boolean> result = new CWResult<Boolean>();
            if(client.getResponseCode() == 200) {
                return result.setSuccess(true);
            }
            else {
                return result.setError("There was an error deleting the message on the server");
            }
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
        return "There was an error deleting the message on the server.";
    }
}
