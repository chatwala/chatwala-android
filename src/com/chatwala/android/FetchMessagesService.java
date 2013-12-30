package com.chatwala.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.GetMessagesForUserCommand;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/30/13
 * Time: 1:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class FetchMessagesService extends Service
{
    private Timer timer;
    private final long delay = 1000;
    private final long period = 1000 * 60;//1000 * 60 * 60 * 2;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        timer = new Timer();
        timer.scheduleAtFixedRate(new MessageFetchTask(), period, period);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        addCommand();
        return super.onStartCommand(intent, flags, startId);
    }

    private void addCommand()
    {
        Log.d("########", "Fetching messages from Service");
        DataProcessor.runProcess(new Runnable()
        {
            @Override
            public void run()
            {
                BusHelper.submitCommandSync(FetchMessagesService.this, new GetMessagesForUserCommand());
            }
        });
    }

    class MessageFetchTask extends TimerTask
    {
        @Override
        public void run()
        {
            addCommand();
        }
    }
}
