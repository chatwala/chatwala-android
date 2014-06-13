package com.chatwala.android.messages;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import com.chatwala.android.db.DatabaseHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 1:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserLoader extends AsyncTaskLoader<List<ChatwalaUser>> {
    private List<ChatwalaUser> users;

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
    public List<ChatwalaUser> loadInBackground() {
        try {
            List<ChatwalaUser> messages = new ArrayList<ChatwalaUser>();
            Dao<ChatwalaMessage, String> messageDao = DatabaseHelper.get().getChatwalaMessageDao();
            QueryBuilder<ChatwalaMessage,String> raw = messageDao.queryBuilder();
            raw.selectRaw("senderId", "timestamp", "userImageUrl", "SUM(CASE WHEN messageState = 'UNREAD' THEN 1 ELSE 0 END)", "MAX(timestamp)");
            raw.groupByRaw("senderId");
            raw.having("COUNT(walaDownloaded) > 0 AND isDeleted = 0");
            raw.orderByRaw("timestamp DESC");
            for(String[] a : raw.queryRaw().getResults()) {
                String senderId = a[0];
                long timestamp = Long.parseLong(a[1]);
                String thumbnailUrl = a[2];
                boolean isUnread = !a[3].equals("0");

                /*QueryBuilder<ChatwalaMessage,String> checkWalasDownloaded = messageDao.queryBuilder();
                checkWalasDownloaded.selectRaw("COUNT(walaDownloaded)");
                checkWalasDownloaded.where().eq("senderId", senderId).and()
                        .eq("walaDownloaded", true);
                if(checkWalasDownloaded.queryRawFirst()[0].equals("0")) {
                    continue;
                }*/

                /*QueryBuilder<ChatwalaMessage,String> checkUnread = messageDao.queryBuilder();
                checkUnread.selectRaw("COUNT(messageState)");
                checkUnread.where().eq("senderId", senderId).and()
                                    .eq("messageState", ChatwalaMessage.MessageState.UNREAD);
                if(!checkUnread.queryRawFirst()[0].equals("0")) {
                    isUnread = true;
                }*/

                messages.add(new ChatwalaUser(senderId, timestamp, thumbnailUrl, isUnread));
            }
            return messages;
        }
        catch(Exception e) {
            return null;
        }
    }

    @Override
    public void deliverResult(List<ChatwalaUser> users) {
        if(isReset()) {
            if(users != null) {
                onReleaseResources(users);
            }
        }
        List<ChatwalaUser> oldUsers = this.users;
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
    public void onCanceled(List<ChatwalaUser> users) {
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

    private void onReleaseResources(List<ChatwalaUser> users) {
        users = null;
    }
}
