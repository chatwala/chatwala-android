package com.chatwala.android;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
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
public class FetchMessagesService extends IntentService
{
    private static Timer timer;
    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = 1000 * 60;

    public FetchMessagesService()
    {
        super("FetchMessagesService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        DataProcessor.runProcess(new Runnable()
        {
            @Override
            public void run()
            {
                Log.d("########", "Running FetchMessagesService");
                BusHelper.submitCommandSync(FetchMessagesService.this, new GetMessagesForUserCommand());
            }
        });
    }

    public static void init(Context context, int minutes)
    {
        Log.d("########", "Initializing FetchMessagesService");
        timer = new Timer();
        timer.scheduleAtFixedRate(new MessageFetchTask(context), ONE_SECOND, ONE_MINUTE * minutes);
    }

    static class MessageFetchTask extends TimerTask
    {
        Context context;

        MessageFetchTask(Context context)
        {
            this.context = context;
        }

        @Override
        public void run()
        {
            Log.d("########", "Starting FetchMessagesService");
            context.startService(new Intent(context, FetchMessagesService.class));
        }
    }
}
