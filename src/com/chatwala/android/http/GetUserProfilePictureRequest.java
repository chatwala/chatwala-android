package com.chatwala.android.http;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import co.touchlab.android.superbus.PermanentException;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.util.*;
import com.squareup.picasso.Picasso;
import com.turbomanage.httpclient.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/3/14
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserProfilePictureRequest extends BaseGetRequest
{
    String userId;

    public GetUserProfilePictureRequest(Context context, String userId)
    {
        super(context);
        this.userId = userId;
    }

    @Override
    protected String getResourceURL()
    {
        return "users/" + userId + "/picture";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        File imageFile = MessageDataStore.findUserImageInLocalStore(userId);

        try
        {
            //Log.d("!!!!!!!!!!!!!!!!", response.getBodyAsString());
            InputStream is = new ByteArrayInputStream(response.getBody());
            FileOutputStream os = new FileOutputStream(imageFile);

            IOUtils.copy(is, os);

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

        ThumbUtils.createThumbForUserImage(context, userId);
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;
    }
}
