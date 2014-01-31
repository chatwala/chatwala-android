package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.loaders.BroadcastSender;
import com.j256.ormlite.dao.Dao;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
    String localMessageUrl;
    String messageId, originalMessageId;

    ChatwalaMessage message;

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
        return "messages/" + messageId + "/uploadURL";
    }

    @Override
    protected byte[] getBytesToPut()
    {
        Log.d("############ Putting local message", localMessageUrl);
        File walaFile = new File(localMessageUrl);

        FileInputStream fileInputStream;

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
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return bFile;
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
    protected void makeAssociatedRequests() throws PermanentException, TransientException
    {
        new PostFinalizeMessageRequest(context, messageId, message.getSenderId(), message.getRecipientId());
    }
}
