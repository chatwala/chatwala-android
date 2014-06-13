package com.chatwala.android.util;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

public class DefaultUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Context context;

    public DefaultUncaughtExceptionHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Logger.e("We had an unexpected crash", ex);

        new Thread() {
            @Override
            public void run() {
                try {
                    Looper.prepare();
                    Toast.makeText(context, "Chatwala crashed", Toast.LENGTH_LONG).show();
                    Looper.loop();
                    Looper.myLooper().quit();
                }
                catch(Exception e) {
                    Logger.e("Got an exception while showing the death toast", e);
                }
            }
        }.start();

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {}

        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
    }
}
