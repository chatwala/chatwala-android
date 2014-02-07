package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.database.DatabaseHelper;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
        putFileToUrl(sasUrl, getBytesToPut(), isPngImage());
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
    protected abstract boolean isPngImage();

    public static void putFileToUrl(String sasUrl, byte[] bytesToPut, boolean contentTypeImage) throws TransientException
    {
        try
        {
            URL url = new URL(sasUrl);
            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("x-ms-blob-type", "BlockBlob");
            urlConnection.setRequestProperty("Content-Type", contentTypeImage ? "image/png" : "application/octet-stream");
            urlConnection.setRequestProperty("Content-Length", Integer.toString(bytesToPut.length));
            urlConnection.setRequestMethod("PUT");

            urlConnection.getOutputStream().write(bytesToPut);
            urlConnection.getOutputStream().close();

            //Returns 201
            Log.d("############", "PUT resp code: " + urlConnection.getResponseCode());

            if(urlConnection.getResponseCode() != 201)
            {
                throw new TransientException("Put failed, retrying.");
            }
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

    public static byte[] convertMessageToBytes(String localMessageFileUrl)
    {
        Log.d("############ Putting local message", localMessageFileUrl);
        File walaFile = new File(localMessageFileUrl);

        FileInputStream fileInputStream;

        byte[] bFile = new byte[(int) walaFile.length()];

        try {
            //convert file into array of bytes
            fileInputStream = new FileInputStream(walaFile);
            fileInputStream.read(bFile);
            fileInputStream.close();

            for (int i = 0; i < bFile.length; i++) {
                System.out.print((char)bFile[i]);
            }
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return bFile;
    }
}
