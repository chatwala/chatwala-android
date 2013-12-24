package com.chatwala.android.http;

import android.content.Context;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.ChatMessage;
import com.chatwala.android.MessageMetadata;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.util.CWLog;
import com.chatwala.android.util.ShareUtils;
import com.chatwala.android.util.ZipUtil;
import com.turbomanage.httpclient.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 11:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessageFileRequest extends BaseGetRequest
{
    private String messageId;
    private ChatMessage chatMessage;
    private ChatwalaMessage chatwalaMessage;

    public GetMessageFileRequest(Context context, String messageId)
    {
        super(context);
        this.messageId = messageId;
    }

    @Override
    protected String getResourceURL()
    {
        return "messages/" + messageId;
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        try
        {
            //Log.d("!!!!!!!!!!!!!!!!", response.getBodyAsString());
            InputStream is = new ByteArrayInputStream(response.getBody());
            File file = new File(context.getFilesDir(), "vid_" + messageId + ".wala");
            FileOutputStream os = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int count;

            while ((count = is.read(buffer)) > 0)
            {
                os.write(buffer, 0, count);
                //Log.d("@@@@@@@@@@@@", new String(buffer, "UTF-8"));
            }

            os.close();
            is.close();

            File outFolder = new File(context.getFilesDir(), "chat_" + messageId);
            outFolder.mkdirs();

            ZipUtil.unzipFiles(file, outFolder);

            chatMessage = new ChatMessage();

            chatMessage.messageVideo = new File(outFolder, "video.mp4");
            FileInputStream input = new FileInputStream(new File(outFolder, "metadata.json"));
            chatMessage.metadata = new MessageMetadata();
            chatMessage.metadata.init(new JSONObject(IOUtils.toString(input)));

            input.close();
        }
        catch (FileNotFoundException e)
        {
            CWLog.b(ShareUtils.class, messageId);
            CWLog.softExceptionLog(ShareUtils.class, "Couldn't read file", e);
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
        chatwalaMessage = databaseHelper.getChatwalaMessageDao().queryForId(messageId);

        if(chatwalaMessage == null)
        {
            chatwalaMessage = new ChatwalaMessage();
            chatwalaMessage.setMessageId(messageId);
            chatwalaMessage.setSortId(null);
        }

        chatwalaMessage.setSenderId(chatMessage.metadata.senderId);
        chatwalaMessage.setRecipientId(AppPrefs.getInstance(context).getUserId());

        databaseHelper.getChatwalaMessageDao().createOrUpdate(chatwalaMessage);
        return chatwalaMessage;
    }

    @Override
    protected Object getReturnValue()
    {
        return chatMessage;
    }
}
