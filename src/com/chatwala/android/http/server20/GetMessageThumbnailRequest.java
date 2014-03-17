package com.chatwala.android.http.server20;

import android.content.Context;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.http.BaseGetRequest;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.ThumbUtils;
import com.turbomanage.httpclient.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.*;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/3/14
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessageThumbnailRequest extends BaseGetRequest
{
    ChatwalaMessage message;

    public GetMessageThumbnailRequest(Context context, ChatwalaMessage message)
    {
        super(context);
        this.message = message;
    }

    @Override
    protected String getResourceURL()
    {
        return message.getThumbnailUrl();
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        File imageFile = MessageDataStore.findUserImageInLocalStore("");

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

        ThumbUtils.createThumbForUserImage(context, "");
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;
    }
}
