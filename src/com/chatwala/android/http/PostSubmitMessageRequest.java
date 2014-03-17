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
import com.chatwala.android.superbus.PutMessageFileWithSasCommand;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.VideoUtils;
import com.chatwala.android.util.ZipUtil;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostSubmitMessageRequest extends BasePostRequest<ChatwalaMessage>
{
    ChatwalaMessage messageMetadata, originalMessage;
    String recipientId, originalMessageId, messagePath;
    VideoUtils.VideoMetadata videoMetadata;

    String generatedMessageId;

    public PostSubmitMessageRequest(Context context, String videoPath, String recipientId, String originalMessageId, VideoUtils.VideoMetadata videoMetadata)
    {
        super(context);
        this.messagePath = videoPath;
        this.recipientId = recipientId;
        this.originalMessageId = originalMessageId;
        this.videoMetadata = videoMetadata;
        generatedMessageId = UUID.randomUUID().toString();
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
        return "messages/" + generatedMessageId;
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        JSONObject bodyAsJson = new JSONObject(response.getBodyAsString());

        ChatwalaMessage chatwalaMessage = new ChatwalaMessage();

        chatwalaMessage.setMessageId(generatedMessageId);
        chatwalaMessage.setUrl(bodyAsJson.getString("sasUrl"));
        chatwalaMessage.setSortId(null);

        //Set at end, in case things got weird parsing
        messageMetadata = chatwalaMessage;

        Logger.i("Posted message has ID " + messageMetadata.getMessageId());
    }

    @Override
    protected boolean hasDbOperation()
    {
        return videoMetadata != null;
    }

    @Override
    protected ChatwalaMessage commitResponse(DatabaseHelper databaseHelper) throws SQLException
    {
        originalMessage = databaseHelper.getChatwalaMessageDao().queryForId(originalMessageId);
        return null;
    }

    @Override
    protected void makeAssociatedRequests() throws PermanentException, TransientException
    {
        if(videoMetadata != null)
        {
            //File outFile = ZipUtil.buildZipToSend(context, new File(messagePath), originalMessage, videoMetadata, messageMetadata.getMessageId());
            //BusHelper.submitCommandSync(context, new PutMessageFileWithSasCommand(outFile.getPath(), messageMetadata.getMessageId(), messageMetadata.getUrl(), originalMessageId, recipientId));
        }
    }

    @Override
    protected ChatwalaMessage getReturnValue()
    {
        return messageMetadata;
    }
}
