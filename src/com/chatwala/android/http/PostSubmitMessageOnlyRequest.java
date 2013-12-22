package com.chatwala.android.http;

import android.content.Context;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.superbus.PutMessageFileCommand;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/21/13
 * Time: 6:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostSubmitMessageOnlyRequest extends PostSubmitMessageRequest
{
    public PostSubmitMessageOnlyRequest(Context context, String localMessagePath, String recipientId)
    {
        super(context, localMessagePath, recipientId);
    }

    @Override
    protected void makeAssociatedRequests() throws PermanentException, TransientException
    {
        if(localMessagePath != null)
            BusHelper.submitCommandSync(context, new PutMessageFileCommand(localMessagePath, messageMetadata.getMessageId()));
    }

}
