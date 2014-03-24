package com.chatwala.android;

/**
 * Created by Eliezer on 3/20/2014.
 */
public class CWResult<T> {
    private boolean success = true;
    private String message;
    private T result;

    public CWResult() {}

    public CWResult(boolean initialSuccess) {
        this(initialSuccess, null);
    }

    public CWResult(boolean initialSuccess, String initialMessage) {
        this.success = initialSuccess;
        this.message = initialMessage;
    }

    public CWResult(CWResult<?> other) {
        this(other.isSuccess(), other.getMessage());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getResult() {
        return result;
    }

    public CWResult<T> setSuccess(T result) {
        return setSuccess(result, null);
    }

    public CWResult<T> setSuccess(T result, String message) {
        success = true;
        this.result = result;
        this.message = message;
        return this;
    }

    public CWResult<T> setError(String message) {
        success = false;
        this.message = message;
        result = null;
        return this;
    }
}
