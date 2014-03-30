package com.chatwala.android.http;

import android.content.Context;
import android.os.Environment;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.superbus.ClearStoreCommand;
import com.chatwala.android.superbus.server20.GetMessageThumbnailCommand;
import com.chatwala.android.superbus.server20.GetMessageUserThumbnailCommand;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.ZipUtil;
import com.turbomanage.httpclient.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private boolean successfulDownload = true;

    public GetMessageFileRequest(Context context, ChatwalaMessage messageMetadata)
    {
        super(context);

        this.chatwalaMessage = messageMetadata;
    }

    @Override
    protected String getResourceURL()
    {
        return chatwalaMessage.getReadUrl();
    }

    protected boolean ignoreBaseURL() {
        return true;
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        File file = null;
        try
        {
            //Log.d("!!!!!!!!!!!!!!!!", response.getBodyAsString());
            InputStream is = new ByteArrayInputStream(response.getBody());
            file = MessageDataStore.makeMessageWalaFile();
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
        catch (Exception e)
        {
            if(file != null)
            {
                file.delete();
            }

            chatwalaMessage = null;
            successfulDownload = false;
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
        return successfulDownload;
    }

    @Override
    protected Object commitResponse(DatabaseHelper databaseHelper) throws SQLException
    {
        boolean exists = databaseHelper.getChatwalaMessageDao().idExists(chatwalaMessage.getMessageId());

        if(!exists)
        {
            //chatwalaMessage.setTimestamp(System.currentTimeMillis());
            chatwalaMessage.setMessageState(ChatwalaMessage.MessageState.UNREAD);

            //Logger.i("New message metadata " + metadataJson.toString());
            //chatwalaMessage.saveMetadata(databaseHelper);
            try {
                chatwalaMessage.populateFromMetaDataJSON(metadataJson);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else
        {
            ChatwalaMessage existingMessage = databaseHelper.getChatwalaMessageDao().queryForId(chatwalaMessage.getMessageId());
            chatwalaMessage = existingMessage;
        }

        chatwalaMessage.setWalaDownloaded(true);

        File thumbnailFile = new File(chatwalaMessage.getThumbnailUrl());
        if(thumbnailFile.exists()) {
            thumbnailFile.setLastModified(0);
        }

        chatwalaMessage.setMessageFile(messageFile);
        databaseHelper.getChatwalaMessageDao().createOrUpdate(chatwalaMessage);

        BroadcastSender.makeNewMessagesBroadcast(context);

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
        BusHelper.submitCommandSync(context, new GetMessageUserThumbnailCommand(chatwalaMessage));
        BusHelper.submitCommandSync(context, new GetMessageThumbnailCommand(chatwalaMessage));
        BusHelper.submitCommandSync(context, new ClearStoreCommand());
    }
}
