package com.chatwala.android.http;

import android.content.Context;
import android.os.Environment;
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
    private ChatwalaMessage chatwalaMessage;

    private File messageFile;
    private JSONObject metadataJson;

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

            IOUtils.copy(is, os);

            os.close();
            is.close();

            File outFolder = new File(context.getFilesDir(), "chat_" + messageId);
            outFolder.mkdirs();

            ZipUtil.unzipFiles(file, outFolder);

//            copyToPublicDrive(outFolder);

            messageFile = new File(outFolder, "video.mp4");
            FileInputStream input = new FileInputStream(new File(outFolder, "metadata.json"));
            metadataJson = new JSONObject(IOUtils.toString(input));

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

    private void copyToPublicDrive(File outFolder) throws IOException
    {
        File[] openedFiles = outFolder.listFiles();
        File outdir = new File(Environment.getExternalStorageDirectory(), outFolder.getName());
        outdir.mkdirs();

        for (File openedFile : openedFiles)
        {
            FileInputStream input = new FileInputStream(openedFile);
            FileOutputStream output = new FileOutputStream(new File(outdir, openedFile.getName()));
            IOUtils.copy(input, output);
            input.close();
            output.close();
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

        chatwalaMessage.setMessageFile(messageFile);

        try
        {
            chatwalaMessage.initMetadata(metadataJson);
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }

        chatwalaMessage.saveMetadata(databaseHelper);
        databaseHelper.getChatwalaMessageDao().createOrUpdate(chatwalaMessage);
        return chatwalaMessage;
    }

    @Override
    protected ChatwalaMessage getReturnValue()
    {
        return chatwalaMessage;
    }
}
