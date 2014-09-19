package com.chatwala.android.events;

import com.chatwala.android.messages.ChatwalaMessageBase;
import com.chatwala.android.util.CwResult;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/9/2014
 * Time: 3:31 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseChatwalaMessageEvent<T extends ChatwalaMessageBase> extends ResultEvent<T> {
    public BaseChatwalaMessageEvent(String id, CwResult<T> message) {
        super(id, message);
    }

    public BaseChatwalaMessageEvent(String id, int extra, CwResult<T> message) {
        super(id, extra, message);
    }

    public BaseChatwalaMessageEvent(String id, int extra) {
        super(id, extra);
    }
}
