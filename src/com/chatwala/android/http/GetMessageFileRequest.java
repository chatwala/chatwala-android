package com.chatwala.android.http;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.R;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.ClearStoreCommand;
import com.chatwala.android.superbus.GetUserProfilePictureCommand;
import com.chatwala.android.util.CWLog;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.ShareUtils;
import com.chatwala.android.util.ZipUtil;
import com.squareup.picasso.Picasso;
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
    private ChatwalaMessage chatwalaMessage;

    private File messageFile;
    private JSONObject metadataJson;

    public GetMessageFileRequest(Context context, ChatwalaMessage messageMetadata)
    {
        super(context);
        this.chatwalaMessage = messageMetadata;
    }

    @Override
    protected String getResourceURL()
    {
        return "messages/" + chatwalaMessage.getMessageId();
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        try
        {
            //Log.d("!!!!!!!!!!!!!!!!", response.getBodyAsString());
            InputStream is = new ByteArrayInputStream(response.getBody());
            File file = MessageDataStore.makeMessageWalaFile();
            FileOutputStream os = new FileOutputStream(file);

            IOUtils.copy(is, os);

            os.close();
            is.close();

            File outFolder = MessageDataStore.makeMessageChatDir();
            outFolder.mkdirs();

            ZipUtil.unzipFiles(file, outFolder);

//            copyToPublicDrive(outFolder);

            messageFile = MessageDataStore.makeVideoFile(outFolder);
            FileInputStream input = new FileInputStream(MessageDataStore.makeMetadataFile(outFolder));
            metadataJson = new JSONObject(IOUtils.toString(input));

            input.close();

            file.delete();
        }
        catch (FileNotFoundException e)
        {
            CWLog.b(ShareUtils.class, chatwalaMessage.getMessageId());
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
        boolean exists = databaseHelper.getChatwalaMessageDao().idExists(chatwalaMessage.getMessageId());

        if(!exists)
        {
            chatwalaMessage.setTimestamp(System.currentTimeMillis());
            chatwalaMessage.setMessageState(ChatwalaMessage.MessageState.UNREAD);

            try
            {
                chatwalaMessage.initMetadata(metadataJson);
            }
            catch (JSONException e)
            {
                throw new RuntimeException(e);
            }

            Log.d("###########", "New message metadata: " + metadataJson.toString());
            chatwalaMessage.saveMetadata(databaseHelper);
        }
        else
        {
            ChatwalaMessage existingMessage = databaseHelper.getChatwalaMessageDao().queryForId(chatwalaMessage.getMessageId());
            chatwalaMessage = existingMessage;
        }

        chatwalaMessage.setMessageFile(messageFile);
        databaseHelper.getChatwalaMessageDao().createOrUpdate(chatwalaMessage);
        return chatwalaMessage;
    }

    @Override
    protected ChatwalaMessage getReturnValue()
    {
        return chatwalaMessage;
    }

    @Override
    protected void makeAssociatedRequests() throws PermanentException, TransientException
    {
        BusHelper.submitCommandSync(context, new GetUserProfilePictureCommand(chatwalaMessage.getSenderId()));
        BusHelper.submitCommandSync(context, new ClearStoreCommand());
    }
}
