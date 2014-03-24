package com.chatwala.android;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbsManager {
    private final ExecutorService queue;
    private ChatwalaApplication app;

    protected AbsManager() {
        queue = Executors.newSingleThreadExecutor();
    }

    protected AbsManager(int numThreads) {
        queue = Executors.newFixedThreadPool(numThreads);
    }

    protected ExecutorService getQueue() {
        return queue;
    }

    public void attachToApplication(ChatwalaApplication app) {
        this.app = app;
    }

    protected ChatwalaApplication getApp() {
        return app;
    }
}
