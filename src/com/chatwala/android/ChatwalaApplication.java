package com.chatwala.android;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import co.touchlab.android.superbus.*;
import co.touchlab.android.superbus.log.BusLog;
import co.touchlab.android.superbus.network.ConnectionChangeBusEventListener;
import co.touchlab.android.superbus.provider.PersistedApplication;
import co.touchlab.android.superbus.provider.PersistenceProvider;
import co.touchlab.android.superbus.provider.gson.GsonSqlitePersistenceProvider;
import co.touchlab.android.superbus.provider.sqlite.SQLiteDatabaseFactory;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.GetMessagesForUserCommand;
import com.chatwala.android.superbus.GetRegisterUserCommand;
import com.chatwala.android.util.SharedPrefsUtils;
import com.crashlytics.android.Crashlytics;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/5/13
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaApplication extends Application implements PersistedApplication
{
    static final String FONT_DIR = "fonts/";
    private static final String ITCAG_DEMI = "ITCAvantGardeStd-Demi.otf",
                ITCAG_MD = "ITCAvantGardeStd-Md.otf";

    public Typeface fontMd;
    public Typeface fontDemi;

    private boolean splashRan;

    private GsonSqlitePersistenceProvider persistenceProvider;

    @Override
    public void onCreate()
    {
        super.onCreate();

        Crashlytics.start(this);

        try
        {
            persistenceProvider = new GsonSqlitePersistenceProvider(new MyDatabaseFactory());
        }
        catch (StorageException e)
        {
            Crashlytics.logException(e);
            throw new RuntimeException(e);
        }

        fontMd = Typeface.createFromAsset(getAssets(), FONT_DIR + ITCAG_MD);
        fontDemi = Typeface.createFromAsset(getAssets(), FONT_DIR + ITCAG_DEMI);

        if(SharedPrefsUtils.getUserId(ChatwalaApplication.this) == null)
        {
            DataProcessor.runProcess(new Runnable()
            {
                @Override
                public void run()
                {
                    BusHelper.submitCommandSync(ChatwalaApplication.this, new GetRegisterUserCommand());
                }
            });
        }

        DataProcessor.runProcess(new Runnable()
        {
            @Override
            public void run()
            {
                BusHelper.submitCommandSync(ChatwalaApplication.this, new GetMessagesForUserCommand());
            }
        });
    }

    public boolean isSplashRan()
    {
        return splashRan;
    }

    public void setSplashRan(boolean splashRan)
    {
        this.splashRan = splashRan;
    }

    @Override
    public PersistenceProvider getProvider()
    {
        return persistenceProvider;
    }

    /**
     * Implement later when we add crash logs.
     * @return
     */
    @Override
    public BusLog getLog()
    {
        return null;
    }

    @Override
    public SuperbusEventListener getEventListener()
    {
        return new ConnectionChangeBusEventListener();
    }

    /**
     * Default to forever.
     * @return
     */
    @Override
    public CommandPurgePolicy getCommandPurgePolicy()
    {
        return null;
    }

    /**
     * Note a foreground service.
     * @return
     */
    @Override
    public ForegroundNotificationManager getForegroundNotificationManager()
    {
        return null;
    }

    private final class MyDatabaseFactory implements SQLiteDatabaseFactory
    {
        @Override
        public SQLiteDatabase getDatabase()
        {
            return DatabaseHelper.getInstance(ChatwalaApplication.this).getWritableDatabase();
        }
    }
}
