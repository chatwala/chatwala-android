package com.chatwala.android.http.server20;

import android.content.Context;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.http.BasePostRequest;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.superbus.GetMessageFileCommand;
import com.j256.ormlite.dao.Dao;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;

import java.sql.SQLException;

/**
 * Created by samirahman on 3/11/14.
 */
public class GetUserInboxRequest extends BasePostRequest {


    private ChatwalaResponse<ChatwalaMessagePage> chatwalaResponse=null;

    public GetUserInboxRequest(Context context) {
        super(context);
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException {

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("user_id", AppPrefs.getInstance(context).getUserId());
        return bodyJson;
    }

    @Override
    protected String getResourceURL() {
        return "messages/userInbox";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException {
        chatwalaResponse = new ChatwalaResponse<ChatwalaMessagePage>();

        JSONObject bodyJson = new JSONObject(response.getBodyAsString());
        JSONObject response_code = bodyJson.getJSONObject("response_code");
        JSONArray messages = bodyJson.getJSONArray("messages");
        boolean shouldContinue = bodyJson.getBoolean("continue");
        String first_id = bodyJson.optString("first_id", null);

        chatwalaResponse.setResponseCode(response_code.getInt("code"));
        chatwalaResponse.setResponseMessage(response_code.getString("message"));

        ChatwalaMessagePage page = new ChatwalaMessagePage();
        page.setContinuePaging(shouldContinue);
        page.setContinueId(first_id);

        ArrayList<ChatwalaMessage> chatwalaMessages= new ArrayList<ChatwalaMessage>();
        for(int i=0; i<messages.length(); i++) {
            JSONObject message_meta_data = messages.getJSONObject(i);
            ChatwalaMessage currentMessage = new ChatwalaMessage();
            currentMessage.populateFromMetaDataJSON(message_meta_data);
            currentMessage.setWriteUrl(bodyJson.getString("write_url"));
            currentMessage.setMessageMetaDataString(message_meta_data.toString(4));
            chatwalaMessages.add(currentMessage);
        }
        page.setMessages(chatwalaMessages);

        chatwalaResponse.setResponseData(page);
    }

    protected ChatwalaResponse<ChatwalaMessagePage> getReturnValue()
    {
        return chatwalaResponse;
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

        ArrayList<ChatwalaMessage> messages = chatwalaResponse.getResponseData().getMessages();
        for(final ChatwalaMessage message : messages)
        {
            boolean exists = databaseHelper.getChatwalaMessageDao().idExists(message.getMessageId());
            if(exists)
            {
                //If a message was first in the chain, not all of this may be filled out
                final ChatwalaMessage updatedMessage = messageDao.queryForId(message.getMessageId());
                updatedMessage.setRecipientId(message.getRecipientId());
                updatedMessage.setSenderId(message.getSenderId());
                updatedMessage.setThumbnailUrl(message.getThumbnailUrl());
                updatedMessage.setSortId(message.getSortId());
                updatedMessage.setGroupId(message.getGroupId());
                updatedMessage.setThreadId(message.getThreadId());
                updatedMessage.setThreadIndex(message.getThreadIndex());
                updatedMessage.setReadUrl(message.getReadUrl());
                updatedMessage.setTimestamp(message.getTimestamp());
                updatedMessage.setReplyingToMessageId(message.getReplyingToMessageId());
                messageDao.update(updatedMessage);
            }
            else
            {
                DataProcessor.runProcess(new Runnable() {
                    @Override
                    public void run() {
                        BusHelper.submitCommandSync(context, new GetMessageFileCommand(message));
                    }
                });
            }
        }

        BroadcastSender.makeNewMessagesBroadcast(context);

        return messages;
    }
}
