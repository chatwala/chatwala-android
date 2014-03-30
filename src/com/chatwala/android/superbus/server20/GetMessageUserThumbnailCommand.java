package com.chatwala.android.superbus.server20;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.http.server20.GetMessageUserThumbnailRequest;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.util.MessageDataStore;

import java.io.File;

/**
 * Created by Eliezer on 3/30/2014.
 */
public class GetMessageUserThumbnailCommand extends SqliteCommand {
    ChatwalaMessage message;

    public GetMessageUserThumbnailCommand(){}

    public GetMessageUserThumbnailCommand(ChatwalaMessage message)
    {
        this.message = message;
    }

    @Override
    public String logSummary()
    {
        return "GetMessageUserThumbnailCommand";
    }

    @Override
    public boolean same(Command command)
    {
        if(command instanceof GetMessageThumbnailCommand)
        {
            if(message.getMessageId().equals(((GetMessageUserThumbnailCommand) command).getMessage().getMessageId()))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        File thumb = MessageDataStore.findMessageUserThumbPathInLocalStore(message.getUserThumbnailUrl());

        //only get the thumb if it's been cached for more than 1 minutes
        if(!thumb.exists() || System.currentTimeMillis()-thumb.lastModified() > 1000*60*1)
        {
            if(thumb.exists()) {
                thumb.setLastModified(System.currentTimeMillis());
            }
            new GetMessageUserThumbnailRequest(context, message).execute();

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
