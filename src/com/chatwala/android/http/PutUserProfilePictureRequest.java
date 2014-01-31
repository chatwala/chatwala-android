package com.chatwala.android.http;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.VideoUtils;
import com.turbomanage.httpclient.HttpResponse;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/3/14
 * Time: 2:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class PutUserProfilePictureRequest extends BaseGetRequest
{
    String filePath;
    Boolean isPicture;

    public PutUserProfilePictureRequest(Context context, String filePath, boolean isPicture)
    {
        super(context);
        this.filePath = filePath;
        this.isPicture = isPicture;
    }

    protected byte[] getImageFileBytes()
    {
        if(isPicture)
        {
            try
            {
                return FileUtils.readFileToByteArray(new File(filePath));
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            Log.d("#######", "Getting the put data for " + filePath);
            Bitmap frame = VideoUtils.createVideoFrame(filePath, 1);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            frame.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] toReturn = stream.toByteArray();

            InputStream is = new ByteArrayInputStream(stream.toByteArray());
            File file = MessageDataStore.makeUserFile(AppPrefs.getInstance(context).getUserId());
            try
            {
                FileOutputStream os = new FileOutputStream(file);


                final byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }

                os.close();
                is.close();
            }
            catch (FileNotFoundException e)
            {
                throw new RuntimeException(e);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            return toReturn;
        }
    }

    @Override
    protected String getResourceURL()
    {
        return "users/" + AppPrefs.getInstance(context).getUserId() + "/pictureUploadURL";
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

            urlConnection.getOutputStream().write(getImageFileBytes());
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
        if(!isPicture)
        {
            new File(filePath).delete();
        }

        return null;
    }
}
