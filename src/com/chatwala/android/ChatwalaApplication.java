package com.chatwala.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import co.touchlab.android.superbus.*;
import co.touchlab.android.superbus.log.BusLog;
import co.touchlab.android.superbus.network.ConnectionChangeBusEventListener;
import co.touchlab.android.superbus.provider.PersistedApplication;
import co.touchlab.android.superbus.provider.PersistenceProvider;
import co.touchlab.android.superbus.provider.gson.GsonSqlitePersistenceProvider;
import co.touchlab.android.superbus.provider.sqlite.SQLiteDatabaseFactory;
import com.chatwala.android.activity.KillswitchActivity;
import com.chatwala.android.activity.SettingsActivity;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.db.DbHelper;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.superbus.CheckKillswitchCommand;
import com.chatwala.android.superbus.PostRegisterPushTokenCommand;
import com.chatwala.android.util.*;
import com.crashlytics.android.Crashlytics;
import xmlwise.Plist;
import xmlwise.XmlParseException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/5/13
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaApplication extends Application implements PersistedApplication, Application.ActivityLifecycleCallbacks
{
    public static final String LOG_TAG = "Chatwala";

    static final String FONT_DIR = "fonts/";
    private static final String ITCAG_DEMI = "ITCAvantGardeStd-Demi.otf",
                ITCAG_MD = "ITCAvantGardeStd-Md.otf";

    public Typeface fontMd;
    public Typeface fontDemi;

    private GsonSqlitePersistenceProvider persistenceProvider;

    public static AtomicBoolean isKillswitchShowing;
    public static int numActivities=0;

    @Override
    public void onCreate()
    {
        super.onCreate();

        Crashlytics.start(this);

        CWAnalytics.initAnalytics(this);

        DbHelper.initInstance(getApplicationContext());

        this.registerActivityLifecycleCallbacks(this);

        if(!MessageDataStore.init(ChatwalaApplication.this))
        {
            Logger.w("There might not be enough space");
        }

        try
        {
            persistenceProvider = new GsonSqlitePersistenceProvider(new MyDatabaseFactory());
        }
        catch (StorageException e)
        {
            Logger.e("Couldn't start the persistence provider");
            throw new RuntimeException(e);
        }

        fontMd = Typeface.createFromAsset(getAssets(), FONT_DIR + ITCAG_MD);
        fontDemi = Typeface.createFromAsset(getAssets(), FONT_DIR + ITCAG_DEMI);

        AppPrefs prefs = AppPrefs.getInstance(ChatwalaApplication.this);
        if(prefs.getUserId() == null)
        {
            String userId = UUID.randomUUID().toString();
            prefs.setUserId(userId);
            Logger.i("User id is " + userId);
        }

        if(!isChatwalaSmsEnabled()) {
            if(AppPrefs.getInstance(getApplicationContext()).getDeliveryMethod() == SettingsActivity.DeliveryMethod.CWSMS) {
                AppPrefs.getInstance(getApplicationContext()).setDeliveryMethod(SettingsActivity.DeliveryMethod.SMS);
            }
        }

        isKillswitchShowing = new AtomicBoolean(false);
        isKillswitchActive(ChatwalaApplication.this);

        DataProcessor.runProcess(new Runnable()
        {
            @Override
            public void run()
            {
                BusHelper.submitCommandSync(ChatwalaApplication.this, new CheckKillswitchCommand());

                if(GCMUtils.shouldRegisterForGcm(ChatwalaApplication.this))
                {
                    BusHelper.submitCommandSync(ChatwalaApplication.this, new PostRegisterPushTokenCommand());
                }
            }
        });

        FetchMessagesService.init(ChatwalaApplication.this, AppPrefs.getInstance(ChatwalaApplication.this).getPrefMessageLoadInterval());
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

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        numActivities++;
        if(numActivities==1) {
            CWAnalytics.sendAppOpenEvent();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        numActivities--;
        if(numActivities==0) {
            CWAnalytics.sendAppBackgroundEvent();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    private final class MyDatabaseFactory implements SQLiteDatabaseFactory
    {
        @Override
        public SQLiteDatabase getDatabase()
        {
            return DatabaseHelper.getInstance(ChatwalaApplication.this).getWritableDatabase();
        }
    }

    public static boolean isChatwalaSmsEnabled() {
        try {
            File killswitchFile = MessageDataStore.makePlistFile();

            if(killswitchFile.exists()) {
                Map<String, Object> properties = Plist.load(killswitchFile);
                Boolean val = (Boolean) properties.get("SMS_DISABLED");
                if(val != null && val) {
                    return false;
                }
                else {
                    return true;
                }
            }
            else {
                return true;
            }
        }
        catch(Exception e) {
            Logger.e("There was an error checking if ChatwalaSMS is enabled", e);
            return true;
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
                Logger.d("Killswitch enabled is " + properties.get("APP_DISABLED"));
                Logger.d("Killswitch text is " + properties.get("AAPP_DISABLED_TEXT"));

                if((Boolean)properties.get("APP_DISABLED"))
                {
                    KillswitchActivity.startMe(context, (String) properties.get("APP_DISABLED_TEXT"));
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
            Logger.e("Unable to parse killswitch plist file", e);
        }
        catch (IOException e)
        {
            Logger.e("IO Exception with killswitch plist file", e);
        }

        return false;
    }
}
