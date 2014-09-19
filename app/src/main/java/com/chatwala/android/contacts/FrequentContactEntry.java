package com.chatwala.android.contacts;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 5:53 PM
 * To change this template use File | Settings | File Templates.
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

