package com.chatwala.android.util;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 3:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class CwResult<T> {
    private boolean success = true;
    private String message;
    private T result;

    public CwResult() {}

    public CwResult(T result) {
        this(true);
        this.result = result;
    }

    public CwResult(boolean initialSuccess) {
        this(initialSuccess, null);
    }

    public CwResult(boolean initialSuccess, String initialMessage) {
        this.success = initialSuccess;
        this.message = initialMessage;
    }

    public CwResult(CwResult<?> other) {
        this(other.isSuccess(), other.getMessage());
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return !success;
    }

    public String getMessage() {
        return message;
    }

    public T getResult() {
        return result;
    }

    public CwResult<T> setSuccess(T result) {
        return setSuccess(result, null);
    }

    public CwResult<T> setSuccess(T result, String message) {
        success = true;
        this.result = result;
        this.message = message;
        return this;
    }

    public CwResult<T> setError(String message) {
        success = false;
        this.message = message;
        result = null;
        return this;
    }
}
