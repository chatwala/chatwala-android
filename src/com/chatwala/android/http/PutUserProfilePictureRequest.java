package com.chatwala.android.http;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.VideoUtils;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;

import java.io.*;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/3/14
 * Time: 2:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class PutUserProfilePictureRequest extends BasePutRequest
{
    String videoPath;

    public PutUserProfilePictureRequest(Context context, String videoPath)
    {
        super(context);
        this.videoPath = videoPath;
    }

    @Override
    String getContentType()
    {
        return "image/png";
    }

    @Override
    protected byte[] getPutData() throws PermanentException, TransientException
    {
        Log.d("#######", "Getting the put data for " + videoPath);
        Bitmap frame = VideoUtils.createVideoFrame(videoPath, 1);
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
            throw new PermanentException(e);
        }
        catch (IOException e)
        {
            throw new TransientException(e);
        }

        return toReturn;
    }

    @Override
    protected String getResourceURL()
    {
        return "users/" + AppPrefs.getInstance(context).getUserId() + "/picture";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        new File(videoPath).delete();
    }
}
