package com.chatwala.android.superbus.server20;

import android.content.Context;
import android.os.Message;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.http.server20.GetUserPictureUploadURLRequest;
import com.chatwala.android.http.server20.ChatwalaResponse;
import com.chatwala.android.http.BaseSasPutRequest;
import com.chatwala.android.util.MessageDataStore;
import org.apache.commons.io.IOUtils;
import com.chatwala.android.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/3/14
 * Time: 2:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadUserProfilePictureCommand extends SqliteCommand
{
    private String path;
    private boolean isPicture;


    public UploadUserProfilePictureCommand(){}

    public UploadUserProfilePictureCommand(String path)
    {
        this.path = path;
    }

    @Override
    public String logSummary()
    {
        return "UploadUserProfilePictureCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return command instanceof UploadUserProfilePictureCommand;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {

        File currentFile = new File(path);
       // File newFile = MessageDataStore.findUserImageInLocalStore(AppPrefs.getInstance(context).getUserId());

        //copy file to user image location
        /*
        try {
            FileOutputStream output = new FileOutputStream(currentFile);
            FileInputStream input = new FileInputStream(newFile);
            IOUtils.copy(input, output);

            input.close();
            output.close();
        }
        catch(IOException e) {

        }*/

        //Get write url
        ChatwalaResponse<String> response= (ChatwalaResponse<String>) new GetUserPictureUploadURLRequest(context).execute();

        Logger.e("MO, do we ever get here!!, responsecode=" + response.getResponseCode());
        if(response.getResponseCode()!=1) {
            throw new TransientException();
        }
        String write_url = response.getResponseData();
        Logger.e("MO, write_url=" + write_url);

        //upload ur
        try {
            byte[] bytes = BaseSasPutRequest.convertFileToBytes(currentFile);
            BaseSasPutRequest.putFileToUrl(write_url, bytes, true);
            Logger.e("MO, user pic uploaded correctly");
        }
        catch(TransientException e) {
            Logger.e("MO, picture upload failed!" + e);
            throw e;
        }



    }
}
