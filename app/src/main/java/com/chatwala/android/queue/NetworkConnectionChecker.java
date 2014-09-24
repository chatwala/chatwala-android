package com.chatwala.android.queue;

import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.requests.MonitorRequest;
import com.chatwala.android.util.Logger;
import com.staticbloc.jobs.BackoffPolicy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Eliezer on 5/8/2014.
 */
public class NetworkConnectionChecker {
    private AtomicBoolean isConnected;
    private long nextCheckTime;
    private int retryCount = 0;
    private BackoffPolicy retryBackoffPolicy = new BackoffPolicy.Constant(1500);

    private static final class Singleton {
        public static final NetworkConnectionChecker instance = new NetworkConnectionChecker();
    }

    private NetworkConnectionChecker() {
        isConnected = new AtomicBoolean(false);
        nextCheckTime = 0;
    }

    public static NetworkConnectionChecker getInstance() {
        return Singleton.instance;
    }

    public synchronized boolean isConnected() {
        if(System.currentTimeMillis() < nextCheckTime) {
            return isConnected.get();
        }
        else {
            nextCheckTime = System.currentTimeMillis() + 30000;
        }

        int responseCode = 0;
        try {
            responseCode = HttpClient.getResponse(new MonitorRequest(), 2000).getResponseCode();
        } catch(Throwable e) {
            Logger.d("Got an exception trying to hit a monitor", e);
        }

        if(responseCode == 200) {
            isConnected.set(true);
            retryCount = 0;
            return true;
        }
        else {
            //TODO
            //we're connected to a network but can't hit monitor
            //try again exponentially backing off
            isConnected.set(false);
            nextCheckTime = System.currentTimeMillis() + retryBackoffPolicy.getNextMillis(++retryCount);
            return false;
        }
    }
}
