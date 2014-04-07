package com.chatwala.android.sms;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Eliezer on 4/3/2014.
 */
public class Sms implements Parcelable {
    private static final int ALLOWED_RETRIES = 6;

    private String number;
    private String message;
    private String messageUrl;
    private String analyticsCategory;
    private int numRetries;

    public Sms(String number, String messageUrl, String analyticsCategory) {
        this(number, SmsManager.DEFAULT_MESSAGE, messageUrl, analyticsCategory);
    }

    public Sms(String number, String message, String messageUrl, String analyticsCategory) {
        this.number = number;
        if(message != null && !message.trim().isEmpty()) {
            this.message = setMessage(message, messageUrl);
        }
        else {
            this.message = SmsManager.DEFAULT_MESSAGE;
        }
        this.messageUrl = messageUrl;
        this.analyticsCategory = analyticsCategory;
        this.numRetries = -1; //this ctor should never be used for a retry
    }

    private String setMessage(String message, String messageUrl) {
        int maxMessageSize = SmsManager.MAX_SMS_MESSAGE_LENGTH - messageUrl.length();
        if(message.length() > maxMessageSize) {
            return message.substring(0, maxMessageSize);
        }
        else {
            return message;
        }
    }


    public Sms(Parcel p) {
        number = p.readString();
        message = p.readString();
        analyticsCategory = p.readString();
        numRetries = p.readInt();
    }

    @Override
    public int hashCode() {
        return (number + message + analyticsCategory).hashCode() ^ (numRetries + 1);
    }

    public String getNumber() {
        return number;
    }

    public String getPrefixMessage() {
        return message;
    }

    public String getMessageUrl() {
        return messageUrl;
    }

    public String getFullMessage() {
        return message + messageUrl;
    }

    public String getAnalyticsCategory() {
        return analyticsCategory;
    }

    public int getNumRetries() {
        return numRetries;
    }

    public boolean canRetry() {
        return numRetries < ALLOWED_RETRIES - 1;
    }

    public long retry() {
        // 1st retry waits 1 minute, then 2, then 4, then 8, then 16, then 32
        // for a total of 6 retries over 63 minutes
        //long base = (long) Math.pow(2, (numRetries + 1)); //base amount of minutes to wait
        long base = (long) 1 << (numRetries + 1); // this is more efficient for raising 2 to x?
        numRetries++; //first retry waits 1 minute
        return (base * 60) * 1000; //convert minutes to milliseconds
    }

    //////////////////////////////
    // Parcelable apis
    //////////////////////////////
    public static final Parcelable.Creator<Sms> CREATOR
            = new Parcelable.Creator<Sms>() {
        public Sms createFromParcel(Parcel p) {
            return new Sms(p);
        }

        public Sms[] newArray(int size) {
            return new Sms[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeString(number);
        p.writeString(message);
        p.writeString(analyticsCategory);
        p.writeInt(numRetries);
    }
    //////////////////////////////
    // end Parcelable apis
    //////////////////////////////
}
