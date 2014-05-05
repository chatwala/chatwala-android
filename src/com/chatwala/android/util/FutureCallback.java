package com.chatwala.android.util;

import java.util.concurrent.FutureTask;

public interface FutureCallback<V> {
    public void runOnMainThread(FutureTask<V> f);
}
