package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;

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
    String messageId;


    public PutMessageFileRequest(Context context, String localMessageUrl, String messageId)
    {
        super(context);
        this.localMessageUrl = localMessageUrl;
        this.messageId = messageId;
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
    protected boolean hasDbOperation()
    {
        return true;
    }

    @Override
    protected Object commitResponse(DatabaseHelper databaseHelper) throws SQLException
    {
        final ChatwalaMessage message = databaseHelper.getChatwalaMessageDao().queryForId(messageId);

        message.clearMessageFile();
        databaseHelper.getChatwalaMessageDao().update(message);

        File walaFile = new File(localMessageUrl);
        walaFile.getParentFile().delete();
        walaFile.delete();

        return null;
    }
}
