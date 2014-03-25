package com.chatwala.android.util;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Eliezer on 3/7/14.
 */
public class Referrer implements Parcelable {
    public static final String FACEBOOK = "fb";
    public static final String MESSAGE = "message";
    public static final String COPY = "copy";

    private static final String NONE = "none";

    private String id;
    private String referrer;
    private String value;

    private boolean isInstallReferrer = false;
    private boolean isAdReferrer = false;

    public Referrer(Uri adReferrer) {
        if("ad".equals(adReferrer.getLastPathSegment())) {
            parseAdReferrer(adReferrer);
        }
        else {
            referrer = NONE;
        }
    }

    public Referrer(String installReferrer) {
        parseInstallReferrer(installReferrer);
    }

    public Referrer(Parcel p) {
        id = p.readString();
        referrer = p.readString();
        value = p.readString();

        isInstallReferrer = (p.readInt() == 1);
        isAdReferrer = (p.readInt() == 1);
    }

    private void parseAdReferrer(Uri adReferrer) {
        this.id = adReferrer.getQueryParameter("id");
        parseReferrerValue(adReferrer.getQueryParameter("referrer"));
        if(!isValid()) {
            return;
        }
        this.isAdReferrer = true;
    }

    private void parseInstallReferrer(String installReferrer) {
        parseReferrerValue(installReferrer);
        if(!isValid()) {
            return;
        }
        this.isInstallReferrer = true;
    }

    private void parseReferrerValue(String referrerStr) {
        if(referrerStr != null) {
            if(FACEBOOK.equals(referrerStr)) {
                referrer = FACEBOOK;
            }
            else if(referrerStr.startsWith(MESSAGE)) {
                referrer = MESSAGE;
                value = referrerStr.substring(7);
            }
            else if(referrerStr.startsWith(COPY)) {
                referrer = COPY;
                value = referrerStr.substring(4);
            }
            else {
                referrer = NONE;
            }
        }
        else {
            referrer = NONE;
        }
    }

    public boolean isInstallReferrer() {
        return isInstallReferrer;
    }

    public boolean isAdReferrer() {
        return isAdReferrer;
    }

    public boolean isValid() {
        return !NONE.equals(referrer);
    }

    public boolean isFacebookReferrer() {
        return FACEBOOK.equals(referrer);
    }

    public boolean isMessageReferrer() {
        return MESSAGE.equals(referrer);
    }

    public boolean isCopyReferrer() {
        return COPY.equals(referrer);
    }

    public String getReferrerString() {
        return referrer;
    }

    public String getValue() {
        return value;
    }

    //////////////////////////////
    // Parcelable apis
    //////////////////////////////
    public static final Parcelable.Creator<Referrer> CREATOR
            = new Parcelable.Creator<Referrer>() {
        public Referrer createFromParcel(Parcel p) {
            return new Referrer(p);
        }

        public Referrer[] newArray(int size) {
            return new Referrer[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeString(id);
        p.writeString(referrer);
        p.writeString(value);
        p.writeInt((isInstallReferrer ? 1 : 0));
        p.writeInt((isAdReferrer ? 1 : 0));
    }
    //////////////////////////////
    // end Parcelable apis
    //////////////////////////////
}
