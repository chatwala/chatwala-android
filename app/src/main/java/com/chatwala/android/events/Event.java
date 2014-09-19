package com.chatwala.android.events;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/9/2014
 * Time: 3:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class Event {
    private String id;
    private int extra;

    public Event(String id) {
        this(id, Extra.UNUSED);
    }

    public Event(String id, int extra) {
        this.id = id;
        this.extra = extra;
    }

    public String getId() {
        return id;
    }

    public int getExtra() {
        return extra;
    }

    public static class Id {
        public static final String UNUSED = "";
    }

    public static class Extra {
        public static final int UNUSED = -1;
        public static final int CANCELED = 1;
        public static final int WALA_BAD_SHARE_ID = 2;
        public static final int WALA_STILL_PUTTING = 3;
        public static final int WALA_GENERIC_ERROR = 4;
        public static final int INVALID_CONVERSATION = 5;
    }
}
