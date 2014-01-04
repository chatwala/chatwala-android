package com.chatwala.android.http;

import android.content.Context;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.util.CWLog;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.ShareUtils;
import com.chatwala.android.util.ZipUtil;
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
        try
        {
            //Log.d("!!!!!!!!!!!!!!!!", response.getBodyAsString());
            InputStream is = new ByteArrayInputStream(response.getBody());
            File file = MessageDataStore.makeUserFile(userId);
            FileOutputStream os = new FileOutputStream(file);

            IOUtils.copy(is, os);

            os.close();
            is.close();
        }
        catch (FileNotFoundException e)
        {

        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;
    }
}
