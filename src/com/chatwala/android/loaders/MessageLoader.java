package com.chatwala.android.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.database.DrawerMessage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

public class MessageLoader extends AsyncTaskLoader<List<DrawerMessage>> {
    private String senderId;

    public MessageLoader(Context context, String senderId) {
        super(context);
        this.senderId = senderId;
    }

    @Override
    public List<DrawerMessage> loadInBackground() {
        try {
            List<DrawerMessage> messages = new ArrayList<DrawerMessage>();
            Dao<ChatwalaMessage, String> messageDao = DatabaseHelper.getInstance(getContext()).getChatwalaMessageDao();
            QueryBuilder<ChatwalaMessage,String> queryBuilder = messageDao.queryBuilder();
            queryBuilder.where().eq("senderId", senderId);
            queryBuilder.orderBy("timestamp", false);
            List<ChatwalaMessage> cwMessages = queryBuilder.query();
            for(ChatwalaMessage message : cwMessages) {
                messages.add(new DrawerMessage(message.getMessageId(), message.getReadUrl(), message.getSenderId(),
                        message.getTimestamp(), message.getThumbnailUrl(), message.getMessageState()));
            }
            return messages;
        }
        catch(Exception e) {
            return null;
        }
    }
}