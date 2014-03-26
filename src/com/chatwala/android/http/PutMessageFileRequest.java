package com.chatwala.android.http;

import android.content.Context;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.superbus.PostFinalizeMessageCommand;
import com.j256.ormlite.dao.Dao;

import java.io.File;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class PutMessageFileRequest extends BaseSasPutRequest
{
    private String localMessageUrl;
    private String messageId, originalMessageId, recipientId, sasUrl;

    ChatwalaMessage message;

    public PutMessageFileRequest(Context context, String localMessageUrl, String messageId, String originalMessageId, String recipientId)
    {
        super(context);
        this.localMessageUrl = localMessageUrl;
        this.messageId = messageId;
        this.originalMessageId = originalMessageId;
        this.recipientId = recipientId;
    }

    @Override
    protected String getResourceURL()
    {
        return "messages/" + messageId + "/uploadURL";
    }

    @Override
    protected byte[] getBytesToPut()
    {
        return convertMessageToBytes(localMessageUrl);
    }

    @Override
    protected void onPutSuccess(DatabaseHelper databaseHelper) throws SQLException
    {
        File walaFile = new File(localMessageUrl);
        File walaDir = walaFile.getParentFile();
        walaFile.delete();
        walaDir.delete();

        if(originalMessageId != null)
        {
            Dao<ChatwalaMessage, String> messageDao = databaseHelper.getChatwalaMessageDao();

            message = messageDao.queryForId(originalMessageId);
            message.setMessageState(ChatwalaMessage.MessageState.REPLIED);
            messageDao.update(message);

            BroadcastSender.makeNewMessagesBroadcast(context);
        }
    }

    @Override
    protected boolean isPngImage()
    {
        return false;
    }

    @Override
    protected void makeAssociatedRequests() throws PermanentException, TransientException
    {
        DataProcessor.runProcess(new Runnable() {
            @Override
            public void run() {
                BusHelper.submitCommandSync(context, new PostFinalizeMessageCommand(messageId, AppPrefs.getInstance(context).getUserId(), recipientId));
            }
        });
    }
}
