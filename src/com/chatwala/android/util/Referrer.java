package com.chatwala.android.util;

import android.net.Uri;

/**
 * Created by Eliezer on 3/7/14.
 */
public class Referrer {
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
        if("ads".equals(adReferrer.getLastPathSegment())) {
            parseAdReferrer(adReferrer);
        }
        else {
            referrer = NONE;
        }
    }

    public Referrer(String installReferrer) {
        parseInstallReferrer(installReferrer);
    }

    private void parseAdReferrer(Uri adReferrer) {
        this.id = adReferrer.getQueryParameter("id");
        parseReferrerValue(adReferrer.getQueryParameter("referrer"));
        if(isNotReferrer()) {
            return;
        }
        this.isAdReferrer = true;
    }

    private void parseInstallReferrer(String installReferrer) {
        parseReferrerValue(installReferrer);
        if(isNotReferrer()) {
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

    public boolean isNotReferrer() {
        return NONE.equals(referrer);
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

    public String getValue() {
        return value;
    }
}
