package com.chatwala.android.migration;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import com.chatwala.android.app.AppPrefs;
import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.db.DatabaseHelper;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.MessageState;
import com.chatwala.android.util.FileUtils;
import com.chatwala.android.util.Logger;
import com.j256.ormlite.dao.Dao;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/20/2014
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class UpdateManager {
    private static final String UPDATE_PREFS = "UPDATE_PREFS";
    private static final String IS_UPDATE_KEY = "IS_UPDATE_%d";
    private static final String LAST_UPDATE_KEY = "LAST_UPDATE";

    private static boolean isUpdating = false;
    private static ExecutorService executor;// = Executors.newSingleThreadExecutor();

    public static boolean isUpdating() {
        return isUpdating;
    }

    private static boolean isUpdate(Context context, int vc) {
        SharedPreferences sp = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE);
        if(!sp.getBoolean(String.format(IS_UPDATE_KEY, vc), false)) {
            sp.edit().putBoolean(String.format(IS_UPDATE_KEY, vc), true).apply();
            return true;
        }
        else {
            return false;
        }
    }

    private static void markVersionAsUpdated(Context context, int vc) {
        SharedPreferences sp = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(String.format(IS_UPDATE_KEY, vc), true).apply();
    }

    private static int getLastVersionUpdated(Context context) {
        SharedPreferences sp = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE);
        return sp.getInt(LAST_UPDATE_KEY, -1);
    }

    private static void setLastVersionUpdated(Context context, int version) {
        SharedPreferences sp = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putInt(LAST_UPDATE_KEY, version).apply();
    }

    public static void updateIfNeeded(final Context context, boolean isFirstOpen) {
        final int currentVersion = ChatwalaApplication.getVersionCode();

        if(isFirstOpen) {
            setLastVersionUpdated(context, currentVersion);
            markVersionAsUpdated(context, currentVersion);
            return;
        }

        if(isUpdate(context, currentVersion)) {
            //if an update has to be done in the background
            /*isUpdating = true;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        int lastVersionUpdated = getLastVersionUpdated(context);
                        if (lastVersionUpdated >= 0) {
                            for (int i = lastVersionUpdated; i <= currentVersion; i++) {
                                doUpdate(context, i);
                            }
                        } else {
                            doUpdate(context, currentVersion);
                        }
                        setLastVersionUpdated(context, currentVersion);
                    }
                    catch(Exception e) {
                        Logger.e("Got an error during migration", e);
                    }
                    finally {
                        isUpdating = false;
                    }
                }
            });*/
            try {
                int lastVersionUpdated = getLastVersionUpdated(context);
                if (lastVersionUpdated >= 0) {
                    for (int i = lastVersionUpdated + 1; i <= currentVersion; i++) {
                        doUpdate(context, i);
                        markVersionAsUpdated(context, i);
                    }
                } else {
                    doUpdate(context, currentVersion);
                    markVersionAsUpdated(context, currentVersion);
                }
                setLastVersionUpdated(context, currentVersion);
            }
            catch(Exception e) {
                Logger.e("Got an error during migration", e);
            }
        }
    }

    private static void doUpdate(Context context, int version) {
        switch(version) {
            case 20082:
                do20082Update(context);
                break;
            default:
                break;
        }
    }

    private static void do20082Update(Context context) {
        //deletes the original legacy db
        context.deleteDatabase("chatwala.db");
        migrateDataFromChatwala2Db(context);
        context.deleteDatabase("chatwala2.db");

        FileUtils.deleteContents(context.getCacheDir());

        AppPrefs.migrateTo20();

        File tempProfilePic = new File(Environment.getExternalStorageDirectory() + "/cwpp.png");
        migrateProfilePic(context, tempProfilePic);

        FileUtils.deleteContents(context.getFilesDir());

        try {
            //create directory for profile pic
            File userDir = new File(context.getFilesDir(), "user");
            userDir.mkdir();
            FileUtils.copy(tempProfilePic, new File(userDir, "user_profile_pic.png"));
        }
        catch(Exception e) {
            Logger.e("There was an error migrating the profile picture", e);
        }
        finally {
            tempProfilePic.delete();
        }
    }

    private static void migrateDataFromChatwala2Db(Context context) {
        Cursor c = null;
        try {
            String query = "SELECT * FROM message";
            MigrateDb2Helper.init(context);
            DbResult<Void> result = new DbResult<Void>();
            DbWrapper db = MigrateDb2Helper.getDbWrapper(result);
            c = db.get().rawQuery(query, null);
            if(c.moveToFirst()) {
                final List<ChatwalaMessage> messages = new ArrayList<ChatwalaMessage>(c.getCount());
                do {
                    String messageId = c.getString(c.getColumnIndex("messageId"));
                    String senderId = c.getString(c.getColumnIndex("senderId"));
                    String recipientId = c.getString(c.getColumnIndex("recipientId"));
                    int sortId = c.getInt(c.getColumnIndex("sortId"));
                    String imageUrl = c.getString(c.getColumnIndex("thumbnailUrl"));
                    String imageModifiedSince = c.getString(c.getColumnIndex("imageModifiedSince"));
                    String userImageModifiedSince = null;
                    String userImageUrl = c.getString(c.getColumnIndex("userThumbnailUrl"));
                    String readUrl = c.getString(c.getColumnIndex("readUrl"));
                    String shareUrl = c.getString(c.getColumnIndex("shareUrl"));
                    String shardKey = c.getString(c.getColumnIndex("shardKey"));
                    long threadIndex = c.getLong(c.getColumnIndex("threadIndex"));
                    String threadId = c.getString(c.getColumnIndex("threadId"));
                    String groupId = c.getString(c.getColumnIndex("groupId"));
                    double startRecording = c.getDouble(c.getColumnIndex("startRecording"));
                    long timestamp = c.getLong(c.getColumnIndex("timestamp"));
                    String messageState = c.getString(c.getColumnIndex("messageState"));
                    String messageMetadataString = c.getString(c.getColumnIndex("messageMetaDataString"));
                    String replyingToMessageId = c.getString(c.getColumnIndex("replyingToMessageId"));
                    boolean walaDownloaded = true;
                    boolean isDeleted = c.getInt(c.getColumnIndex("messageId")) == 1;

                    ChatwalaMessage message = new ChatwalaMessage();
                    message.setMessageId(messageId);
                    message.setSenderId(senderId);
                    message.setRecipientId(recipientId);
                    message.setSortId(sortId);
                    message.setImageUrl(imageUrl);
                    message.setImageModifiedSince(imageModifiedSince);
                    message.setUserImageModifiedSince(userImageModifiedSince);
                    message.setUserImageUrl(userImageUrl);
                    message.setReadUrl(readUrl);
                    message.setShareUrl(shareUrl);
                    message.setShardKey(shardKey);
                    message.setThreadIndex(threadIndex);
                    message.setThreadId(threadId);
                    message.setGroupId(groupId);
                    message.setStartRecording(startRecording);
                    message.setTimestamp(timestamp);
                    if("UNREAD".equals(messageState)) {
                        message.setMessageState(MessageState.UNREAD);
                    }
                    else if("READ".equals(messageState)) {
                        message.setMessageState(MessageState.READ);
                    }
                    else if("REPLIED".equals(messageState)) {
                        message.setMessageState(MessageState.REPLIED);
                    }
                    else {
                        message.setMessageState(MessageState.UNREAD);
                    }
                    message.setMessageMetadataString(messageMetadataString);
                    message.setReplyingToMessageId(replyingToMessageId);
                    message.setWalaDownloaded(walaDownloaded);
                    message.setDeleted(isDeleted);
                    messages.add(message);
                } while(c.moveToNext());
                DatabaseHelper helper = DatabaseHelper.get();
                final Dao<ChatwalaMessage, String> dao = helper.getChatwalaMessageDao();
                dao.callBatchTasks(new Callable<Void>() {
                    public Void call() throws Exception {
                        for(ChatwalaMessage message : messages) {
                            dao.createOrUpdate(message);
                        }
                        Logger.i("Finished inserting migrated data");
                        return null;
                    }
                });
                Logger.i("Inserting migrated data");
            }
            else {
                Logger.e("Didn't find any data while migrating from chatwala2");
            }
        }
        catch(Exception e) {
            Logger.e("Couldn't migrate data from chatwala2");
        }
        finally {
            if(c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    private static void migrateProfilePic(Context context, File tempProfilePic) {
        try {
            File profilePicDir = new File(context.getFilesDir() + "/images/profile");
            File profilePic = profilePicDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    try {
                        return file.getName().endsWith(".png");
                    }
                    catch(Exception ignore) {
                        return false;
                    }
                }
            })[0];
            if(profilePic != null && profilePic.exists()) {
                FileUtils.copy(profilePic, tempProfilePic);
            }
        }
        catch(Exception e) {
            Logger.e("Couldn't migrate profile pic", e);
        }
    }
}

