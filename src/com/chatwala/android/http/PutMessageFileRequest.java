package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.loaders.BroadcastSender;
import com.j256.ormlite.dao.Dao;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class PutMessageFileRequest extends BasePutRequest
{
    String localMessageUrl;
    String messageId, originalMessageId;


    public PutMessageFileRequest(Context context, String localMessageUrl, String messageId, String originalMessageId)
    {
        super(context);
        this.localMessageUrl = localMessageUrl;
        this.messageId = messageId;
        this.originalMessageId = originalMessageId;
    }

    @Override
    protected String getResourceURL()
    {
        return "messages/" + messageId;
    }

    @Override
    protected String getContentType()
    {
        return "application/zip";
    }

    @Override
    protected byte[] getPutData()
    {
        Log.d("############ Putting local message", localMessageUrl);
        File walaFile = new File(localMessageUrl);

        try
        {
            FileInputStream fileInputStream = new FileInputStream(walaFile);

            byte[] bFile = new byte[(int) walaFile.length()];

            try {
                //convert file into array of bytes
                fileInputStream = new FileInputStream(walaFile);
                fileInputStream.read(bFile);
                fileInputStream.close();

                for (int i = 0; i < bFile.length; i++) {
                    System.out.print((char)bFile[i]);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            return bFile;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        File walaFile = new File(localMessageUrl);
        walaFile.getParentFile().delete();
        walaFile.delete();
    }

    @Override
    protected boolean hasDbOperation()
    {
        return originalMessageId != null;
    }

    @Override
    protected Object commitResponse(DatabaseHelper databaseHelper) throws SQLException
    {
        Dao<ChatwalaMessage, String> messageDao = databaseHelper.getChatwalaMessageDao();

        ChatwalaMessage message = messageDao.queryForId(originalMessageId);
        message.setMessageState(ChatwalaMessage.MessageState.REPLIED);
        messageDao.update(message);

        BroadcastSender.makeNewMessagesBroadcast(context);

        return null;
    }
}
