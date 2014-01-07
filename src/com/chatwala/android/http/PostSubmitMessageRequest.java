package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.superbus.PutMessageFileCommand;
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
public class PostSubmitMessageRequest extends BasePostRequest<ChatwalaMessage>
{
    ChatwalaMessage messageMetadata;
    String localMessagePath;
    String recipientId;

    public PostSubmitMessageRequest(Context context, String localMessagePath, String recipientId)
    {
        super(context);
        this.localMessagePath = localMessagePath;
        this.recipientId = recipientId;
    }

    @Override
    protected JSONObject makeBodyJson() throws JSONException, SQLException
    {
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("recipient_id", recipientId != null ? recipientId : "unknown_recipient");
        bodyJson.put("sender_id", AppPrefs.getInstance(context).getUserId());
        return bodyJson;
    }

    @Override
    protected String getResourceURL()
    {
        return "messages";
    }

    public ChatwalaMessage getMessageMetadata()
    {
        return messageMetadata;
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        JSONObject bodyAsJson = new JSONObject(response.getBodyAsString());

        ChatwalaMessage chatwalaMessage = new ChatwalaMessage();

        chatwalaMessage.setMessageId(bodyAsJson.getString("message_id"));
        chatwalaMessage.setUrl(bodyAsJson.getString("url"));
        chatwalaMessage.setSortId(null);

        //Set at end, in case things got weird parsing
        messageMetadata = chatwalaMessage;

        Log.d("###########", "POSTED MESSAGE HAS ID" + messageMetadata.getMessageId());
    }

    @Override
    protected boolean hasDbOperation()
    {
        return false;
    }

    @Override
    protected void makeAssociatedRequests() throws PermanentException, TransientException
    {
        if(localMessagePath != null)
            BusHelper.submitCommandSync(context, new PutMessageFileCommand(localMessagePath, messageMetadata.getMessageId()));
    }

    @Override
    protected ChatwalaMessage getReturnValue()
    {
        return messageMetadata;
    }
}
