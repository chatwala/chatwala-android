package com.chatwala.android.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.database.DrawerUser;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eliezer on 3/25/2014.
 */
public class UserLoader extends AsyncTaskLoader<List<DrawerUser>> {
    public UserLoader(Context context) {
        super(context);
    }

    @Override
    public List<DrawerUser> loadInBackground() {
        try {
            List<DrawerUser> messages = new ArrayList<DrawerUser>();
            Dao<ChatwalaMessage, String> messageDao = DatabaseHelper.getInstance(getContext()).getChatwalaMessageDao();
            QueryBuilder<ChatwalaMessage,String> raw = messageDao.queryBuilder();
            raw.selectRaw("senderId", "timestamp", "thumbnailUrl", "MAX(timestamp)");
            raw.groupByRaw("senderId");
            raw.orderByRaw("timestamp");
            for(String[] a : raw.queryRaw().getResults()) {
                String senderId = a[0];
                long timestamp = Long.parseLong(a[1]);
                String thumbnailUrl = a[2];
                boolean isUnread = false;

                QueryBuilder<ChatwalaMessage,String> checkUnread = messageDao.queryBuilder();
                checkUnread.selectRaw("COUNT(messageState)");
                checkUnread.where().eq("senderId", senderId).eq("messageState", "UNREAD");
                if(!checkUnread.queryRawFirst()[0].equals("0")) {
                    isUnread = true;
                }

                messages.add(new DrawerUser(senderId, timestamp, thumbnailUrl, isUnread));
            }
            return messages;
        }
        catch(Exception e) {
            return null;
        }
    }
}
