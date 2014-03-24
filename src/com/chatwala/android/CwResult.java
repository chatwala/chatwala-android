package com.chatwala.android;

/**
 * Created by Eliezer on 3/20/2014.
 */
public class CwResult<T> {
    private boolean success = true;
    private boolean partial = false;
    private String message;
    private T result;

    public CwResult() {}

    public CwResult(boolean initialSuccess) {
        this(initialSuccess, null);
    }

    public CwResult(boolean initialSuccess, String initialMessage) {
        this.success = initialSuccess;
        this.message = initialMessage;
    }

    public CwResult(boolean initialSuccess, boolean isPartialSuccess, String initialMessage) {
        this.success = initialSuccess;
        this.partial = isPartialSuccess;
        this.message = initialMessage;
    }

    public CwResult(CwResult<?> other) {
        this(other.isSuccess(), other.isPartial(), other.getMessage());
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return !success;
    }

    public boolean isPartial() {
        return partial;
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

    public void setPartial(boolean partial) {
        this.partial = partial;
    }
}
