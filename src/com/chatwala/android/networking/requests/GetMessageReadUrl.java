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
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Eliezer on 3/27/2014.
 */
public class GetMessageReadUrl implements Request<HttpURLConnection, GetMessageReadUrlResponse> {
    private String shareId;


    public GetMessageReadUrl(String shareId) {
        this.shareId = shareId;
    }

    private String getUrl() {
        return EnvironmentVariables.get().getApiPath() + "messages/getReadUrlFromShareId";
    }

    @Override
    public NetworkCallable<HttpURLConnection, GetMessageReadUrlResponse> getCallable(Context context, int numRetries) {
        return new NetworkCallable<HttpURLConnection, GetMessageReadUrlResponse>(context, this, numRetries);
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
        request.put("share_id", shareId);

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
    public CWResult<GetMessageReadUrlResponse> parseResponse(HttpURLConnection client) throws Exception {
        StringBuilder responseBuilder = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String line;
            while((line = in.readLine()) != null) {
                responseBuilder.append(line);
            }
            in.close();

            JSONObject responseBody = new JSONObject(responseBuilder.toString());

            int code = responseBody.getJSONObject("response_code").optInt("code", -1);
            String readUrl = responseBody.getString("read_url");
            String messageId = responseBody.getString("message_id");

            GetMessageReadUrlResponse getMessageReadUrlResponse = new GetMessageReadUrlResponse(code, messageId, readUrl);
            CWResult<GetMessageReadUrlResponse> response = new CWResult<GetMessageReadUrlResponse>().setSuccess(getMessageReadUrlResponse);

            NetworkLogger.logHttpResponse(client, getUrl(), response);
            return response;
        }
        catch(ConnectException ce) {
            GetMessageReadUrlResponse getMessageReadUrlResponse = new GetMessageReadUrlResponse(-2, null, null);
            return new CWResult<GetMessageReadUrlResponse>().setSuccess(getMessageReadUrlResponse);
        }
        catch(FileNotFoundException fnfe) {
            GetMessageReadUrlResponse getMessageReadUrlResponse = new GetMessageReadUrlResponse(-1, null, null);
            return new CWResult<GetMessageReadUrlResponse>().setSuccess(getMessageReadUrlResponse);
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
        return "This is not a valid message.";
    }
}
