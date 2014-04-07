package com.chatwala.android.contacts;

/**
 * Created by Eliezer on 3/31/2014.
 */
public class FrequentContactEntry extends ContactEntry {
    private int timesContacted;

    public FrequentContactEntry(String name, String value, String type, String image, int timesContacted, boolean isContact) {
        super(name, value, type, image, isContact);
        this.timesContacted = timesContacted;
    }

    public int getTimesContacted() { return timesContacted; }

    public boolean equals(ContactEntry other) {
        return compareTo(other) == 0;
    }

    @Override
    public int compareTo(ContactEntry other) {
        FrequentContactEntry mostContactedOther = (FrequentContactEntry) other;
        if(getTimesContacted() > mostContactedOther.getTimesContacted()) {
            return -1;
        }
        else if(getTimesContacted() == mostContactedOther.getTimesContacted()) {
            return 0;
        }
        else {
            return 1;
        }
    }
}
