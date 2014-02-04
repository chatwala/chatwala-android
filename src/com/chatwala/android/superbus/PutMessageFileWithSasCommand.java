package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.http.BaseSasPutRequest;
import com.chatwala.android.loaders.BroadcastSender;
import com.j256.ormlite.dao.Dao;

import java.io.File;

/**
 * Created by matthewdavis on 2/4/14.
 */
public class PutMessageFileWithSasCommand extends SqliteCommand
{
    private String messageId, sasUrl, messageLocalUrl, originalMessageId, recipientId;

    public PutMessageFileWithSasCommand(){}

    public PutMessageFileWithSasCommand(String messageLocalUrl, String messageId, String sasUrl, String originalMessageId, String recipientId)
    {
        this.messageId = messageId;
        this.sasUrl = sasUrl;
        this.messageLocalUrl = messageLocalUrl;
        this.originalMessageId = originalMessageId;
        this.recipientId = recipientId;
    }

    @Override
    public String logSummary()
    {
        return "PutMessageFileWithSasCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return false;
    }

    @Override
    public void callCommand(final Context context) throws TransientException, PermanentException
    {
        try
        {
            BaseSasPutRequest.putFileToUrl(sasUrl, BaseSasPutRequest.convertMessageToBytes(messageLocalUrl));

            File walaFile = new File(messageLocalUrl);
            File walaDir = walaFile.getParentFile();
            walaFile.delete();
            walaDir.delete();

            if(originalMessageId != null)
            {
                Dao<ChatwalaMessage, String> messageDao = DatabaseHelper.getInstance(context).getChatwalaMessageDao();

                ChatwalaMessage message = messageDao.queryForId(originalMessageId);
                message.setMessageState(ChatwalaMessage.MessageState.REPLIED);
                messageDao.update(message);

                BroadcastSender.makeNewMessagesBroadcast(context);
            }

            DataProcessor.runProcess(new Runnable() {
                @Override
                public void run() {
                    BusHelper.submitCommandSync(context, new PostFinalizeMessageCommand(messageId, AppPrefs.getInstance(context).getUserId(), recipientId));
                }
            });
        }
        catch(Exception e)
        {
            DataProcessor.runProcess(new Runnable()
            {
                @Override
                public void run()
                {
                    BusHelper.submitCommandSync(context, new PutMessageFileCommand(messageLocalUrl, messageId, originalMessageId, recipientId));
                }
            });
        }
    }
}
