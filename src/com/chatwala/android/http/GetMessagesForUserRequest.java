package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.GetMessageFileCommand;
import com.chatwala.android.util.SharedPrefsUtils;
import com.j256.ormlite.dao.Dao;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 2:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessagesForUserRequest extends BaseGetRequest
{
    ArrayList<ChatwalaMessage> messageArray;

    public GetMessagesForUserRequest(Context context)
    {
        super(context);
    }

    @Override
    protected String getResourceURL()
    {
        return "users/" + SharedPrefsUtils.getUserId(context) + "/messages";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        JSONObject bodyJson = new JSONObject(response.getBodyAsString());
        JSONArray messagesJsonArray = bodyJson.getJSONArray("messages");

        messageArray = new ArrayList<ChatwalaMessage>();

        Log.d("#######", "Parsing messages, total: " + messagesJsonArray.length());
        for(int i=0; i < messagesJsonArray.length(); i++)
        {
            JSONObject messageJson = messagesJsonArray.getJSONObject(i);
            ChatwalaMessage currentMessage = new ChatwalaMessage();
            currentMessage.setMessageId(messageJson.getString("message_id"));
            currentMessage.setRecipientId(messageJson.getString("recipient_id"));
            currentMessage.setSenderId(messageJson.getString("sender_id"));
            messageArray.add(currentMessage);
            Log.d("######### FINISHED PARSING: ", currentMessage.getMessageId());
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

        for(ChatwalaMessage message : messageArray)
        {
            messageDao.createOrUpdate(message);
        }

        return messageArray;
    }

    @Override
    protected void makeAssociatedRequests() throws PermanentException, TransientException
    {
        for(ChatwalaMessage message : messageArray)
        {
            BusHelper.submitCommandSync(context, new GetMessageFileCommand(message.getMessageId()));
        }
    }
}
