package com.chatwala.android.loaders;

import android.content.Context;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.util.SharedPrefsUtils;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
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
            String userId = SharedPrefsUtils.getUserId(getContext());
            Dao<ChatwalaMessage, String> messageDao = DatabaseHelper.getInstance(getContext()).getChatwalaMessageDao();
            if(userId != null)
            {
                return messageDao.query(messageDao.queryBuilder().where().not().eq("senderId", userId).prepare());
            }
            else
            {
                return messageDao.queryForAll();
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
