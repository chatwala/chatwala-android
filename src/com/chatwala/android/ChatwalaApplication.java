package com.chatwala.android;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.util.Log;
import co.touchlab.android.superbus.*;
import co.touchlab.android.superbus.log.BusLog;
import co.touchlab.android.superbus.network.ConnectionChangeBusEventListener;
import co.touchlab.android.superbus.provider.PersistedApplication;
import co.touchlab.android.superbus.provider.PersistenceProvider;
import co.touchlab.android.superbus.provider.gson.GsonSqlitePersistenceProvider;
import co.touchlab.android.superbus.provider.sqlite.SQLiteDatabaseFactory;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.superbus.CheckKillswitchCommand;
import com.chatwala.android.superbus.GetRegisterUserCommand;
import com.chatwala.android.util.CWLog;
import com.chatwala.android.util.MessageDataStore;
import com.crashlytics.android.Crashlytics;
import xmlwise.Plist;
import xmlwise.XmlParseException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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

        if(!MessageDataStore.init(ChatwalaApplication.this))
        {
            CWLog.b(ChatwalaApplication.class, "There might not be enough space");
        }

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

        isKillswitchActive(ChatwalaApplication.this);

        DataProcessor.runProcess(new Runnable()
        {
            @Override
            public void run()
            {
                BusHelper.submitCommandSync(ChatwalaApplication.this, new CheckKillswitchCommand());
                if(AppPrefs.getInstance(ChatwalaApplication.this).getUserId() == null)
                {
                    BusHelper.submitCommandSync(ChatwalaApplication.this, new GetRegisterUserCommand());
                }
            }
        });

        FetchMessagesService.init(ChatwalaApplication.this, AppPrefs.getInstance(ChatwalaApplication.this).getPrefMessageLoadInterval());
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

    public static boolean isKillswitchActive(Context context)
    {
        try
        {
            File killswitchFile = MessageDataStore.makePlistFile();

            if(killswitchFile.exists())
            {
                Map<String, Object> properties = Plist.load(killswitchFile);
                Log.d("####KILLSWITCH####", Boolean.toString((Boolean) properties.get("APP_DISABLED")));
                Log.d("####KILLSWITCH####", (String) properties.get("APP_DISABLED_TEXT"));

                if((Boolean)properties.get("APP_DISABLED"))
                {
                    KillswitchActivity.startMe(context, (String)properties.get("APP_DISABLED_TEXT"));
                    return true;
                }
                else
                {
                    BroadcastSender.makeKillswitchOffBroadcast(context);
                }
            }

            //todo - what should we do in these cases? For now we'll just eat them
        }
        catch (XmlParseException e)
        {
            CWLog.softExceptionLog(ChatwalaApplication.class, "Unable to parse killswitch plist file", e);
        }
        catch (IOException e)
        {
            CWLog.softExceptionLog(ChatwalaApplication.class, "IO Exception with killswitch plist file", e);
        }

        return false;
    }
}
