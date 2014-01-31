package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.database.DatabaseHelper;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

/**
 * Created by matthewdavis on 1/31/14.
 */
public abstract class BaseSasPutRequest extends BaseGetRequest
{
    public BaseSasPutRequest(Context context)
    {
        super(context);
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException
    {
        String sasUrl = new JSONObject(response.getBodyAsString()).getString("sasUrl");

        try
        {
            URL url = new URL(sasUrl);
            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("x-ms-blob-type", "BlockBlob");
            urlConnection.setRequestMethod("PUT");

            urlConnection.getOutputStream().write(getBytesToPut());
            urlConnection.getOutputStream().close();

            //Returns 201
            Log.d("############", "PUT resp code: " + urlConnection.getResponseCode());
        }
        catch (MalformedURLException e)
        {
            throw new TransientException(e);
        }
        catch (IOException e)
        {
            throw new TransientException(e);
        }
    }

    @Override
    protected boolean hasDbOperation()
    {
        return true;
    }

    @Override
    protected Object commitResponse(DatabaseHelper databaseHelper) throws SQLException
    {
        onPutSuccess(databaseHelper);
        return null;
    }

    protected abstract byte[] getBytesToPut();
    protected abstract void onPutSuccess(DatabaseHelper databaseHelper) throws SQLException;
}
