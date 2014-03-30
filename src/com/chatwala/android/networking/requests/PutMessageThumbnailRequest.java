package com.chatwala.android.networking.requests;

import android.content.Context;
import com.chatwala.android.CWResult;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.networking.NetworkCallable;
import com.chatwala.android.networking.NetworkLogger;
import com.chatwala.android.networking.NetworkManager;
import com.chatwala.android.networking.Request;
import com.chatwala.android.util.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Eliezer on 3/27/2014.
 */
public class PutMessageThumbnailRequest implements Request<HttpURLConnection, Boolean> {
    Context context;
    ChatwalaMessage message;
    URL writeUrl;
    private File thumbnailFile;

    public PutMessageThumbnailRequest(Context context, ChatwalaMessage message, File thumbnailFile) {
        this.context = context;
        this.message = message;
        this.thumbnailFile = thumbnailFile;
    }

    private URL getUrl() throws Exception {
        return NetworkManager.getInstance().postToQueue(new RenewMessageThumbnailWriteUrl(
                message.getMessageId(), message.getShardKey()).getCallable(context, 2)).get().getResult();
    }

    @Override
    public NetworkCallable<HttpURLConnection, Boolean> getCallable(Context context, int numRetries) {
        return new NetworkCallable<HttpURLConnection, Boolean>(context, this, numRetries);
    }

    @Override
    public HttpURLConnection getConnection(boolean isRetry) throws Exception {
        try {
            writeUrl = getUrl();
        }
        catch(Exception e) {
            Logger.e("There was an issue getting the message thumbnail write url");
            throw e;
        }
        HttpURLConnection client = (HttpURLConnection) writeUrl.openConnection();
        client.setDoInput(true);
        client.setDoOutput(true);
        client.setRequestMethod("PUT");
        return client;
    }

    @Override
    public CWResult<Boolean> makeRequest(HttpURLConnection client) throws Exception {
        CWResult<Boolean> result = new CWResult<Boolean>();
        client.setRequestProperty("x-ms-blob-type", "BlockBlob");
        client.setRequestProperty("Content-Type", "image/png");
        client.setRequestProperty("Content-Length", Long.toString(thumbnailFile.length()));

        NetworkLogger.logHttpRequest(client, getUrl().toString(), "<message_thumbnail_binary>");

        try {
            BufferedOutputStream out = new BufferedOutputStream(client.getOutputStream());
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(thumbnailFile));

            int i;
            while((i = in.read()) >= 0) {
                out.write(i);
            }
            out.flush();
            out.close();
            in.close();
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
            InputStreamReader in = new InputStreamReader(client.getInputStream());
            while(in.read() != -1) {}
            in.close();

            CWResult<Boolean> response = new CWResult<Boolean>();
            if(client.getResponseCode() == 201) {
                response.setSuccess(true);
            }
            else {
                response.setError(getGenericErrorMessage());
            }
            NetworkLogger.logHttpResponse(client, writeUrl.toString(), response);
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
        return "There was an error uploading the message thumbnail.";
    }
}
