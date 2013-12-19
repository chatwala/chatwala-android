package com.chatwala.android.http;

import android.content.Context;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostSubmitMessageRequest extends BasePostRequest
{
    ChatwalaMessage messageMetadata;
    String localMessagePath;

    public PostSubmitMessageRequest(Context context, String localMessagePath)
    {
        super(context);
        this.localMessagePath = localMessagePath;
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException
    {
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("recipient_id", "unknown_recipient");
        bodyJson.put("sender_id", "unknown_recipient");
        return bodyJson;
    }

    @Override
    protected String getResourceURL()
    {
        return "messages";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        JSONObject bodyAsJson = new JSONObject(response.getBodyAsString());

        messageMetadata = new ChatwalaMessage();
        messageMetadata.setMessageId(bodyAsJson.getString("message_id"));
        messageMetadata.setUrl(bodyAsJson.getString("url"));
    }

    @Override
    protected boolean hasDbOperation()
    {
        return true;
    }

    @Override
    protected Object commitResponse(DatabaseHelper databaseHelper) throws SQLException
    {
        databaseHelper.getChatwalaMessageDao().create(messageMetadata);
        return messageMetadata;
    }

    @Override
    protected void makeAssociatedRequests() throws PermanentException, TransientException
    {
        new PutMessageFileRequest(context, localMessagePath, messageMetadata.getMessageId()).execute();
//        DataProcessor.runProcess(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                BusHelper.submitCommandSync(context, new PutMessageFileCommand(localMessagePath, messageMetadata.getMessageId()));
//            }
//        });
    }

    @Override
    protected Object getReturnValue()
    {
        return messageMetadata.getUrl();
    }
}
