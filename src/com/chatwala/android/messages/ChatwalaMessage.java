package com.chatwala.android.messages;

import android.os.Parcel;
import android.os.Parcelable;
import com.chatwala.android.db.DatabaseHelper;
import com.chatwala.android.files.FileManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.table.DatabaseTable;
import org.json.JSONObject;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
@DatabaseTable(tableName = "message")
public class ChatwalaMessage extends ChatwalaMessageBase {
    public ChatwalaMessage() {}

    public ChatwalaMessage(String messageId) {
        super(messageId);
    }

    public ChatwalaMessage(JSONObject metadata) {
        super(metadata);
    }

    public ChatwalaMessage(Parcel p) {
        super(p);
    }

    @Override
    public File getLocalVideoFile() {
        return FileManager.getVideoFromInboxMessageDir(this);
    }

    @Override
    public File getLocalMetadataFile() {
        return FileManager.getMetadataFromInboxMessageDir(this);
    }

    @Override
    public File getLocalWalaFile() {
        return FileManager.getWalaFromInboxMessageDir(this);
    }

    @Override
    public File getLocalMessageImage() {
        return FileManager.getMessageImage(this);
    }

    @Override
    public File getLocalMessageThumb() {
        return FileManager.getMessageThumb(this);
    }

    @Override
    public File getLocalUserImage() {
        return FileManager.getUserImage(getSenderId());
    }

    @Override
    public File getLocalUserThumb() {
        return FileManager.getUserThumb(getSenderId());
    }

    @Override
    public Dao<ChatwalaMessage, String> getDao() throws Exception {
        return DatabaseHelper.get().getChatwalaMessageDao();
    }

    //////////////////////////////
    // Parcelable apis
    //////////////////////////////
    public static final Parcelable.Creator<ChatwalaMessage> CREATOR = new Parcelable.Creator<ChatwalaMessage>() {
        public ChatwalaMessage createFromParcel(Parcel in) {
            return new ChatwalaMessage(in);
        }

        public ChatwalaMessage[] newArray(int size) {
            return new ChatwalaMessage[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
    }
    //////////////////////////////
    // end Parcelable apis
    //////////////////////////////
}
