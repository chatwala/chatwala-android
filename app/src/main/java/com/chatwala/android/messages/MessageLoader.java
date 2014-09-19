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
 * Time: 1:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageLoader extends AsyncTaskLoader<List<ChatwalaMessage>> {
    private List<ChatwalaMessage> messages;
    private String senderId;

    public MessageLoader(Context context, String senderId) {
        super(context);
        this.senderId = senderId;
        onContentChanged();
    }

    @Override
    protected void onStartLoading() {
        if(messages != null) {
            deliverResult(messages);
        }

        if(messages == null || takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public List<ChatwalaMessage> loadInBackground() {
        try {
            List<ChatwalaMessage> messages = new ArrayList<ChatwalaMessage>();
            Dao<ChatwalaMessage, String> messageDao = DatabaseHelper.get().getChatwalaMessageDao();
            QueryBuilder<ChatwalaMessage,String> queryBuilder = messageDao.queryBuilder();
            queryBuilder.where().eq("senderId", senderId).and().eq("walaDownloaded", true);
            queryBuilder.orderBy("timestamp", false);
            List<ChatwalaMessage> cwMessages = queryBuilder.query();
            for(ChatwalaMessage message : cwMessages) {
                messages.add(message);
            }
            return messages;
        }
        catch(Exception e) {
            return null;
        }
    }

    @Override
    public void deliverResult(List<ChatwalaMessage> messages) {
        if(isReset()) {
            if(messages != null) {
                onReleaseResources(messages);
            }
        }
        List<ChatwalaMessage> oldMessages = this.messages;
        this.messages = messages;

        if(isStarted()) {
            super.deliverResult(messages);
        }

        if(oldMessages != null) {
            onReleaseResources(oldMessages);
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(List<ChatwalaMessage> messages) {
        super.onCanceled(messages);

        onReleaseResources(messages);
    }

    @Override
    protected void onReset() {
        super.onReset();

        onStopLoading();

        if(messages != null) {
            onReleaseResources(messages);
        }
    }

    private void onReleaseResources(List<ChatwalaMessage> messages) {
        //messages = null;
        //don't do anything
    }
}
