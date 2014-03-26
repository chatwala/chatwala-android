package com.chatwala.android.http;

import android.content.Context;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.ThumbUtils;
import com.turbomanage.httpclient.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
