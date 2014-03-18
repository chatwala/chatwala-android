package com.chatwala.android.http.server20;

import android.content.Context;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.http.BusHttpClient;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.http.BaseGetRequest;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.superbus.GetMessageFileCommand;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.ThumbUtils;
import com.j256.ormlite.dao.Dao;
import com.turbomanage.httpclient.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

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
    String lastModified=null;

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
    protected boolean ignoreBaseURL() {
        return true;
    }

    protected HttpResponse makeRequest(BusHttpClient client)
    {
        client.addHeader("If-Modified-Since", message.getImageModifiedSince());
        return super.makeRequest(client);
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        try
        {
            if(response.getStatus()==304) { //nothing has changed
                return;
            }

            Map<String,List<String>> headers = response.getHeaders();

            if(headers.containsKey("Last-Modified")) {
                lastModified = headers.get("Last-Modified").get(0);
            }

            ThumbUtils.createThumbForMessage(context, response.getBody(), message.getThumbnailUrl());
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
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
        Dao<ChatwalaMessage, String> messageDao = databaseHelper.getChatwalaMessageDao();

        boolean exists = databaseHelper.getChatwalaMessageDao().idExists(message.getMessageId());

        //update last modified
        if(exists && lastModified!=null)
        {
            //If a message was first in the chain, not all of this may be filled out
            ChatwalaMessage updatedMessage = messageDao.queryForId(message.getMessageId());
            updatedMessage.setImageModifiedSince(lastModified);
            messageDao.update(updatedMessage);
        }

        return null;
    }
}
