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
    private List<DrawerUser> users;

    public UserLoader(Context context) {
        super(context);
        onContentChanged();
    }

    @Override
    protected void onStartLoading() {
        if(users != null) {
            deliverResult(users);
        }

        if(users == null || takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public List<DrawerUser> loadInBackground() {
        try {
            List<DrawerUser> messages = new ArrayList<DrawerUser>();
            Dao<ChatwalaMessage, String> messageDao = DatabaseHelper.getInstance(getContext()).getChatwalaMessageDao();
            QueryBuilder<ChatwalaMessage,String> raw = messageDao.queryBuilder();
            raw.selectRaw("senderId", "timestamp", "thumbnailUrl", "MAX(timestamp)");
            raw.groupByRaw("senderId");
            raw.orderByRaw("timestamp DESC");
            for(String[] a : raw.queryRaw().getResults()) {
                String senderId = a[0];
                long timestamp = Long.parseLong(a[1]);
                String thumbnailUrl = a[2];
                boolean isUnread = false;

                QueryBuilder<ChatwalaMessage,String> checkWalasDownloaded = messageDao.queryBuilder();
                checkWalasDownloaded.selectRaw("COUNT(walaDownloaded)");
                checkWalasDownloaded.where().eq("senderId", senderId).and()
                        .eq("walaDownloaded", true);
                if(checkWalasDownloaded.queryRawFirst()[0].equals("0")) {
                    continue;
                }

                QueryBuilder<ChatwalaMessage,String> checkUnread = messageDao.queryBuilder();
                checkUnread.selectRaw("COUNT(messageState)");
                checkUnread.where().eq("senderId", senderId).and()
                                    .eq("messageState", ChatwalaMessage.MessageState.UNREAD);
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

    @Override
    public void deliverResult(List<DrawerUser> users) {
        if(isReset()) {
            if(users != null) {
                onReleaseResources(users);
            }
        }
        List<DrawerUser> oldUsers = this.users;
        this.users = users;

        if(isStarted()) {
            super.deliverResult(users);
        }

        if(oldUsers != null) {
            onReleaseResources(oldUsers);
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(List<DrawerUser> users) {
        super.onCanceled(users);

        onReleaseResources(users);
    }

    @Override
    protected void onReset() {
        super.onReset();

        onStopLoading();

        if(users != null) {
            onReleaseResources(users);
        }
    }

    private void onReleaseResources(List<DrawerUser> users) {
        users = null;
    }
}
