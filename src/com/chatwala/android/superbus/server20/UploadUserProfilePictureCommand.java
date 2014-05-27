package com.chatwala.android.superbus.server20;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;

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

        /*File currentFile = new File(path);

        File newFile = MessageDataStore.findUserImageInLocalStore(AppPrefs.getInstance(context).getUserId());


        if(!currentFile.getPath().equals(newFile.getPath())) {
            try {
                FileOutputStream output = new FileOutputStream(currentFile);
                FileInputStream input = new FileInputStream(newFile);
                IOUtils.copy(input, output);

                input.close();
                output.close();
            }
            catch(IOException e) {

            }
        }

        //Get write url
        ChatwalaResponse<String> response= (ChatwalaResponse<String>) new GetUserPictureUploadURLRequest(context).execute();

        if(response.getResponseCode()!=1) {
            throw new TransientException();
        }
        String write_url = response.getResponseData();

        //upload ur
        try {
            byte[] bytes = BaseSasPutRequest.convertFileToBytes(currentFile);
            BaseSasPutRequest.putFileToUrl(write_url, bytes, true);
        }
        catch(TransientException e) {
            throw e;
        }*/



    }
}
