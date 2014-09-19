package com.chatwala.android.messages;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 5:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageStartInfo implements Parcelable {
    private String messageId;
    private String shareUrl;

    public MessageStartInfo(String messageId, String shareUrl) {
        this.messageId = messageId;
        this.shareUrl = shareUrl;
    }

    public MessageStartInfo(Parcel p) {
        messageId = p.readString();
        shareUrl = p.readString();
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public String getMessageId() {
        return messageId;
    }

    //////////////////////////////
    // Parcelable apis
    //////////////////////////////
    public static final Creator<MessageStartInfo> CREATOR
            = new Creator<MessageStartInfo>() {
        public MessageStartInfo createFromParcel(Parcel p) {
            return new MessageStartInfo(p);
        }

        public MessageStartInfo[] newArray(int size) {
            return new MessageStartInfo[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeString(messageId);
        p.writeString(shareUrl);
    }
    //////////////////////////////
    // end Parcelable apis
    //////////////////////////////
}
