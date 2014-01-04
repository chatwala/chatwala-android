package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.http.PutUserProfilePictureRequest;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/3/14
 * Time: 2:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class PutUserProfilePictureCommand extends SqliteCommand
{
    private String videoPath;

    public PutUserProfilePictureCommand(){}

    public PutUserProfilePictureCommand(String videoPath)
    {
        this.videoPath = videoPath;
    }

    @Override
    public String logSummary()
    {
        return "PutUserProfilePictureCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return command instanceof PutUserProfilePictureCommand;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new PutUserProfilePictureRequest(context, videoPath).execute();
    }
}
