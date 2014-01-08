package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.ChatwalaNotificationManager;
import com.chatwala.android.http.GetUserProfilePictureRequest;
import com.chatwala.android.loaders.BroadcastSender;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/3/14
 * Time: 5:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserProfilePictureCommand extends SqliteCommand
{
    String userId;

    public GetUserProfilePictureCommand(){}

    public GetUserProfilePictureCommand(String userId)
    {
        this.userId = userId;
    }

    @Override
    public String logSummary()
    {
        return "GetUserProfilePictureCommand";
    }

    @Override
    public boolean same(Command command)
    {
        if(command instanceof GetUserProfilePictureCommand)
        {
            if(userId.equals(((GetUserProfilePictureCommand)command).getUserId()))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        new GetUserProfilePictureRequest(context, userId).execute();
    }

    @Override
    public void onSuccess(Context context)
    {
        BroadcastSender.makeNewMessagesBroadcast(context);
    }

    public String getUserId()
    {
        return userId;
    }
}
