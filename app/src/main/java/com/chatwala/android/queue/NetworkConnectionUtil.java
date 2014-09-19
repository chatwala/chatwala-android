package com.chatwala.android.queue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.requests.MonitorRequest;
import com.path.android.jobqueue.network.NetworkEventProvider;
import com.path.android.jobqueue.network.NetworkUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Eliezer on 5/8/2014.
 */
public class NetworkConnectionUtil implements NetworkUtil, NetworkEventProvider {
    private Listener listener;
    private AtomicBoolean isConnected;
    private AtomicLong lastCheckTime;

    public NetworkConnectionUtil(Context context) {
        isConnected = new AtomicBoolean(false);
        lastCheckTime = new AtomicLong(0);
        isConnected(context);

        context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(listener == null) {
                    return;
                }

                listener.onNetworkChange(isConnected(context));
            }
        }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public boolean isConnected(Context context) {
        if(System.currentTimeMillis() - lastCheckTime.get() <= 30000) {
            return isConnected.get();
        }
        else {
            lastCheckTime.set(System.currentTimeMillis());
        }

        context = context.getApplicationContext();

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if(netInfo != null && netInfo.isConnectedOrConnecting()) {
            try {
                if(HttpClient.getResponse(new MonitorRequest(), 2000).getResponseCode() == 200) {
                    isConnected.set(true);
                    return true;
                }
                else {
                    //TODO
                    //we're connected to a network but can't hit monitor
                    //try again exponentially backing off
                    isConnected.set(false);
                    lastCheckTime.set(0);
                    return false;
                }
            }
            catch (Throwable e) {
                //TODO
                //we're connected to a network but can't hit monitor
                //try again exponentially backing off
                isConnected.set(false);
                lastCheckTime.set(0);
                return false;
            }
        }
        else {
            isConnected.set(false);
            lastCheckTime.set(0);
            return false;
        }
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
