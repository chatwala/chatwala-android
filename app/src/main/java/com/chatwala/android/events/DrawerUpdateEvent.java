package com.chatwala.android.events;

import com.staticbloc.events.Event;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/21/2014
 * Time: 7:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class DrawerUpdateEvent extends Event<Integer> {
    public static final int LOAD_EVENT_EXTRA = 0;
    public static final int REFRESH_EVENT_EXTRA = 1;

    public DrawerUpdateEvent(int extra) {
        super(Ids.UNUSED, extra);
    }

    public boolean isLoadEvent() {
        return getExtra() == LOAD_EVENT_EXTRA;
    }

    public boolean isRefreshEvent() {
        return getExtra() == REFRESH_EVENT_EXTRA;
    }
}
