package com.chatwala.android.loaders;

import android.content.Context;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.ThumbUtils;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/19/13
 * Time: 5:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessagesLoader extends BaseAsyncLoader<List<ChatwalaMessage>>
{
    public MessagesLoader(Context context)
    {
        super(context);
    }

    @Override
    protected String getBroadcastString()
    {
        return BroadcastSender.NEW_MESSAGES_BROADCAST;
    }

    @Override
    public List<ChatwalaMessage> loadInBackground()
    {
        try
        {
            //Checking to see if all these files exist every time the loader triggers is not going to scale well
            //todo: consider either an "image_loaded" column on messages or creating a User table with that and other relevant info. Until we're more solid on how thumbs will work, this will be fine.
            Dao<ChatwalaMessage, String> messageDao = DatabaseHelper.getInstance(getContext()).getChatwalaMessageDao();
            //List<ChatwalaMessage> messages = messageDao.query(messageDao.queryBuilder().where().isNotNull("sortId").prepare());
            List<ChatwalaMessage> messages = messageDao.queryForAll();
            for(Iterator<ChatwalaMessage> iterator = messages.iterator(); iterator.hasNext();)
            {
                ChatwalaMessage current = iterator.next();
                if(!MessageDataStore.findUserImageInLocalStore(current.getSenderId()).exists())
                {
                    iterator.remove();
                }

                if(!MessageDataStore.findUserImageThumbInLocalStore(current.getSenderId()).exists())
                {
                    //Should almost never get here, it's for compatibility with images downloaded before we were making thumbs.
                    ThumbUtils.createThumbForUserImage(getContext(), current.getSenderId());
                }
            }
            return messages;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
