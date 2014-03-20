package com.chatwala.android.networking;

import com.chatwala.android.CwResult;

import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by samirahman on 3/20/14.
 */
public class ConnectionExecutor {

    private static final ConnectionExecutor instance = new ConnectionExecutor();

    private final ExecutorService pool;

    private static final int NUM_THREADS = 5;

    private ConnectionExecutor() {
        pool = Executors.newFixedThreadPool(NUM_THREADS);
    }


    public static ConnectionExecutor getInstance() {
        return instance;
    }

    public void execute(final HttpURLConnection connection, final HttpResponseParser<?> parser) {

        Callable<CwResult> callable = new Callable<CwResult>() {
            HttpURLConnection conn = connection;
            HttpResponseParser p = parser;
            @Override
            public CwResult<?> call() throws Exception {
                conn.connect();
                return p.parse(conn);
            }
        };

        pool.submit(callable);
    }
}
