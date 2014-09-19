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
 * Date: 5/11/2014
 * Time: 10:33 PM
 * To change this template use File | Settings | File Templates.
 */
@DatabaseTable(tableName = "sent_messages")
public class ChatwalaSentMessage extends ChatwalaMessageBase {
    public ChatwalaSentMessage() {}

    public ChatwalaSentMessage(String messageId) {
        super(messageId);
    }

    public ChatwalaSentMessage(JSONObject metadata) {
        super(metadata);
    }

    public ChatwalaSentMessage(Parcel p) {
        super(p);
    }

    @Override
    public File getLocalVideoFile() {
        return FileManager.getVideoFromSentMessageDir(this);
    }

    @Override
    public File getLocalMetadataFile() {
        return FileManager.getMetadataFromSentMessageDir(this);
    }

    @Override
    public File getLocalWalaFile() {
        return FileManager.getWalaFromSentMessageDir(this);
    }

    public File getOutboxWalaFile() {
        return FileManager.getWalaFromOutboxMessageDir(this);
    }

    public File getOutboxVideoFile() {
        return FileManager.getVideoFromOutboxMessageDir(this);
    }

    public File getOutboxMetadataFile() {
        return FileManager.getMetadataFromOutboxMessageDir(this);
    }

    @Override
    public File getLocalMessageImage() {
        return FileManager.getSentMessageImage(this);
    }

    @Override
    public File getLocalMessageThumb() {
        return FileManager.getSentMessageThumb(this);
    }

    @Override
    public File getLocalUserImage() {
        return FileManager.getUserImage(getRecipientId());
    }

    @Override
    public File getLocalUserThumb() {
        return FileManager.getUserThumb(getRecipientId());
    }

    @Override
    public Dao<ChatwalaSentMessage, String> getDao() throws Exception {
        return DatabaseHelper.get().getChatwalaSentMessageDao();
    }

    //////////////////////////////
    // Parcelable apis
    //////////////////////////////
    public static final Creator<ChatwalaSentMessage> CREATOR = new Creator<ChatwalaSentMessage>() {
        public ChatwalaSentMessage createFromParcel(Parcel in) {
            return new ChatwalaSentMessage(in);
        }

        public ChatwalaSentMessage[] newArray(int size) {
            return new ChatwalaSentMessage[size];
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
