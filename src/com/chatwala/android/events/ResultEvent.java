package com.chatwala.android.events;

import com.chatwala.android.util.CwResult;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 3:32 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ResultEvent<T> extends Event {
    private CwResult<T> result;

    public ResultEvent(String id, CwResult<T> result) {
        super(id);
        this.result = result;
    }

    public ResultEvent(String id, int extra) {
        super(id, extra);
        result = new CwResult<T>(false);
    }

    public ResultEvent(String id, int extra, CwResult<T> result) {
        super(id, extra);
        this.result = result;
    }

    public boolean isSuccess() {
        return result.isSuccess();
    }

    public boolean isError() {
        return result.isError();
    }

    public T getResult() {
        return result.getResult();
    }

    public String getMessage() {
        return result.getMessage();
    }
}
