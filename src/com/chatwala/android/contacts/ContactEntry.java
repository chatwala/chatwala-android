package com.chatwala.android.contacts;

import android.os.Handler;
import android.os.Looper;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class ContactEntry implements Comparable<ContactEntry> {
    public interface OnSendStateChangedListener {
        public void onSendStateChanged(ContactEntry contact, boolean isSent);
    }

    private String name;
    private String value;
    private String type;
    private String image;
    private boolean isContact;

    private boolean isSending = false;
    private boolean isSent = false;
    private static final int TIME_TO_SEND = 5;
    private int timeToSend = TIME_TO_SEND;

    private OnSendStateChangedListener listener;

    private Handler sendingHandler = new Handler(Looper.getMainLooper());

    private Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            timeToSend--;
            if(timeToSend == 0) {
                sendMessage();
            }
            else {
                if(isSending()) {
                    sendingHandler.postDelayed(this, 1000);
                }
            }
            if(listener != null) {
                listener.onSendStateChanged(ContactEntry.this, isSent());
            }
        }
    };

    public ContactEntry(String name, String value, String type, String image, boolean isContact) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.image = image;
        this.isContact = isContact;
    }

    public String getName() {
        return (name == null ? "" : name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return (value == null ? "" : value);
    }

    public String getType() {
        return (type == null ? "" : type);
    }

    public String getImage() {
        return image;
    }

    public boolean isContact() { return isContact; }

    public boolean equals(ContactEntry other) {
        return compareTo(other) == 0;
    }

    public boolean isSending() {
        return isSending;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setIsSent(boolean isSent) {
        this.isSent = isSent;
    }

    public boolean isSentOrSending() {
        return isSending || isSent;
    }

    public boolean startSend(OnSendStateChangedListener listener) {
        if(isSent) {
            return false;
        }

        isSending = true;
        timeToSend = TIME_TO_SEND;
        this.listener = listener;

        sendingHandler.post(countdownRunnable);
        return true;
    }

    public void cancelSend() {
        isSending = false;
        isSent = false;
        timeToSend = TIME_TO_SEND;
        sendingHandler.removeCallbacks(countdownRunnable);
    }

    public String getSendingStatus() {
        if(isSent) {
            return "Sent";
        }
        else if(isSending) {
            return "Sending " + timeToSend;
        }
        else {
            return "";
        }
    }

    public void sendMessage() {
        isSending = false;
        isSent = true;
    }

    public int hashCode() {
        return (getName() + getValue() + getType()).hashCode();
    }

    @Override
    public int compareTo(ContactEntry other) {
        if(other == null) {
            return -1;
        }

        String name = getName();
        String otherName = other.getName();

        if(!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }

        if(!otherName.isEmpty() && Character.isDigit(otherName.charAt(0))) {
            otherName = "_" + otherName;
        }

        int compare = name.compareToIgnoreCase(otherName);
        if(compare == 0) {
            return getValue().compareToIgnoreCase(other.getValue());
        }
        else {
            return compare;
        }
    }
}

