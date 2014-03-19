package com.chatwala.android.superbus.server20;

import android.content.Context;
import android.os.Message;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.http.server20.GetMessageThumbnailRequest;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.util.MessageDataStore;
import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/3/14
 * Time: 5:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessageThumbnailCommand extends SqliteCommand
{
    ChatwalaMessage message;

    public GetMessageThumbnailCommand(){}

    public GetMessageThumbnailCommand(ChatwalaMessage message)
    {
        this.message = message;
    }

    @Override
    public String logSummary()
    {
        return "GetMessageThumbnailCommand";
    }

    @Override
    public boolean same(Command command)
    {
        if(command instanceof GetMessageThumbnailCommand)
        {
            if(message.getMessageId().equals(((GetMessageThumbnailCommand) command).getMessage().getMessageId()))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
       File thumb = MessageDataStore.findMessageThumbInLocalStore(message.getThumbnailUrl());

        //only get the thumb if it's been cached for more than 1 minutes
        if(System.currentTimeMillis()-thumb.lastModified() > 1000*60*1)
       {
           thumb.setLastModified(System.currentTimeMillis());
            new GetMessageThumbnailRequest(context, message).execute();

       }

    }

    @Override
    public void onSuccess(Context context)
    {
        BroadcastSender.makeNewMessagesBroadcast(context);
    }

    public ChatwalaMessage getMessage()
    {
        return message;
    }
}
