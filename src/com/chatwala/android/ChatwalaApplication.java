package com.chatwala.android;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.CommandPurgePolicy;
import co.touchlab.android.superbus.ForegroundNotificationManager;
import co.touchlab.android.superbus.StorageException;
import co.touchlab.android.superbus.SuperbusEventListener;
import co.touchlab.android.superbus.log.BusLog;
import co.touchlab.android.superbus.network.ConnectionChangeBusEventListener;
import co.touchlab.android.superbus.provider.PersistedApplication;
import co.touchlab.android.superbus.provider.PersistenceProvider;
import co.touchlab.android.superbus.provider.gson.GsonSqlitePersistenceProvider;
import co.touchlab.android.superbus.provider.sqlite.SQLiteDatabaseFactory;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.db.DBHelper;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.networking.NetworkManager;
import com.chatwala.android.superbus.PostRegisterPushTokenCommand;
import com.chatwala.android.util.CWAnalytics;
import com.chatwala.android.util.GCMUtils;
import com.chatwala.android.util.KillswitchInfo;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.MessageDataStore;
import com.crashlytics.android.Crashlytics;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/5/13
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaApplication extends Application implements PersistedApplication, Application.ActivityLifecycleCallbacks {
    public static final String LOG_TAG = "Chatwala";

    static final String FONT_DIR = "fonts/";
    private static final String ITCAG_DEMI = "ITCAvantGardeStd-Demi.otf",
                ITCAG_MD = "ITCAvantGardeStd-Md.otf";

    public Typeface fontMd;
    public Typeface fontDemi;

    private GsonSqlitePersistenceProvider persistenceProvider;

    public static int numActivities=0;

    public NetworkManager networkManager;
    public MessageManager messageManager;

    @Override
    public void onCreate() {
        super.onCreate();

        networkManager = NetworkManager.attachToApp(this);
        messageManager = MessageManager.attachToApp(this);

        Crashlytics.start(this);

        CWAnalytics.initAnalytics(this);

        DBHelper.initInstance(getApplicationContext());

        this.registerActivityLifecycleCallbacks(this);

        if(!MessageDataStore.init(ChatwalaApplication.this)) {
            Logger.w("There might not be enough space");
        }

        try {
            persistenceProvider = new GsonSqlitePersistenceProvider(new MyDatabaseFactory());
        }
        catch (StorageException e) {
            Logger.e("Couldn't start the persistence provider");
            throw new RuntimeException(e);
        }

        fontMd = Typeface.createFromAsset(getAssets(), FONT_DIR + ITCAG_MD);
        fontDemi = Typeface.createFromAsset(getAssets(), FONT_DIR + ITCAG_DEMI);

        AppPrefs prefs = AppPrefs.getInstance(ChatwalaApplication.this);
        if(prefs.getUserId() == null) {
            String userId = UUID.randomUUID().toString();
            prefs.setUserId(userId);
            Logger.i("User id is " + userId);
        }

        try {
            new Thread() {
                @Override
                public void run() {
                    try {
                        KillswitchInfo oldKillswitch = AppPrefs.getInstance(getApplicationContext()).getKillswitch();
                        CWResult<JSONObject> killswitchResult = NetworkManager.getInstance().getKillswitch(oldKillswitch).get();
                        if(killswitchResult.isSuccess()) {
                            AppPrefs.getInstance(getApplicationContext()).putKillswitch(killswitchResult.getResult());
                        }
                        else {
                            AppPrefs.getInstance(getApplicationContext()).putKillswitch(new JSONObject());
                        }
                    }
                    catch(Exception e) {
                        Logger.e("Couldn't get the killswitch", e);
                    }
                }
            }.start();
        }
        catch(Exception e) {
            Logger.e("The killswitch checker thread crashed", e);
        }

        DataProcessor.runProcess(new Runnable() {
            @Override
            public void run() {
                if(GCMUtils.shouldRegisterForGcm(ChatwalaApplication.this)) {
                    BusHelper.submitCommandSync(ChatwalaApplication.this, new PostRegisterPushTokenCommand());
                }
            }
        });

        startService(new Intent(this, FetchMessagesService.class));
    }

    @Override
    public PersistenceProvider getProvider() {
        return persistenceProvider;
    }

    /**
     * Implement later when we add crash logs.
     * @return
     */
    @Override
    public BusLog getLog() {
        return null;
    }

    @Override
    public SuperbusEventListener getEventListener() {
        return new ConnectionChangeBusEventListener();
    }

    /**
     * Default to forever.
     * @return
     */
    @Override
    public CommandPurgePolicy getCommandPurgePolicy() {
        return null;
    }

    /**
     * Note a foreground service.
     * @return
     */
    @Override
    public ForegroundNotificationManager getForegroundNotificationManager() {
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

    private final class MyDatabaseFactory implements SQLiteDatabaseFactory {
        @Override
        public SQLiteDatabase getDatabase() {
            return DatabaseHelper.getInstance(ChatwalaApplication.this).getWritableDatabase();
        }
    }
}
