package com.chatwala.android.app;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.chatwala.android.db.DatabaseHelper;
import com.chatwala.android.files.FileManager;
import com.chatwala.android.files.ImageManager;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.migration.UpdateManager;
import com.chatwala.android.queue.JobQueues;
import com.chatwala.android.queue.jobs.GetUserProfilePictureJob;
import com.chatwala.android.queue.jobs.GetUserSentboxJob;
import com.chatwala.android.queue.jobs.KillswitchJob;
import com.chatwala.android.queue.jobs.RegisterGcmTokenJob;
import com.chatwala.android.sms.SmsManager;
import com.chatwala.android.sms.SmsScanner;
import com.chatwala.android.users.UserManager;
import com.chatwala.android.util.CwAnalytics;
import com.chatwala.android.util.DefaultUncaughtExceptionHandler;
import com.chatwala.android.util.GcmUtils;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.koushikdutta.ion.Ion;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaApplication extends Application implements Application.ActivityLifecycleCallbacks {
    public static int numActivities = 0;
    private static String versionName = "0";
    private static int versionCode = -1;

    private boolean isFirstOpen;

    public CwNotificationManager notificationManager;
    public FileManager fileManager;
    public ImageManager imageManager;
    public MessageManager messageManager;
    public UserManager userManager;
    public SmsManager smsManager;

    private DatabaseHelper dbHelper;

    private Timer analyticTimer;

    /*
        The order of method calls is very important here.
        Don't change them unless you know what you're doing.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(getApplicationContext()));
        Crashlytics.start(this);

        if(getPackageManager() != null && getPackageName() != null) {
            try {
                versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException ignore) {}
            try {
                versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException ignore) {}
        }

        GoogleAnalytics ga = GoogleAnalytics.getInstance(this);
        CwAnalytics.attachToApp(this, ga.newTracker(EnvironmentVariables.get().getGoogleAnalyticsId()));

        dbHelper = DatabaseHelper.attachToApp(this);

        AppPrefs.initPrefs(this);
        isFirstOpen = AppPrefs.isFirstOpen();
        UpdateManager.updateIfNeeded(getApplicationContext(), isFirstOpen);

        JobQueues.attachToApp(this);

        notificationManager = CwNotificationManager.attachToApp(this);
        fileManager = FileManager.attachToApp(this);
        imageManager = ImageManager.attachToApp(this);
        messageManager = MessageManager.attachToApp(this);
        userManager = UserManager.attachToApp(this);
        smsManager = SmsManager.attachToApp(this);

        this.registerActivityLifecycleCallbacks(this);

        if(!UserManager.isUserCreated()) {
            UserManager.createUser();
        }

        if(GcmUtils.shouldRegisterForGcm(getApplicationContext())) {
            RegisterGcmTokenJob.post(getApplicationContext());
        }

        //check if the user changed their profile picture
        GetUserProfilePictureJob.post();

        //sync the user's sent messages with the server
        GetUserSentboxJob.post();

        startService(new Intent(this, FetchMessagesService.class));

        analyticTimer = new Timer();

        initIon();

        if(isFirstOpen()) {
            scanForSmsLinks();
        }
    }

    private void scanForSmsLinks() {
        List<String> scannedSmsLinks = SmsScanner.getRecentChatwalaSmsLinks(getApplicationContext(), 1L, TimeUnit.DAYS);
        if(!scannedSmsLinks.isEmpty()) {
            AppPrefs.setFirstLink(scannedSmsLinks.get(0));
            for(String link : scannedSmsLinks) {
                if(link != null) {
                    MessageManager.getInstance().startGetMessageForShareId(link);
                }
            }
        }
    }

    private void initIon() {
        Ion.getDefault(getApplicationContext()).getBitmapCache().setErrorCacheDuration(0);
    }

    public boolean isFirstOpen() {
        return isFirstOpen;
    }

    public static String getVersionName() {
        return versionName;
    }

    public static int getVersionCode() {
        return versionCode;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        numActivities++;
        if(numActivities == 1) {
            checkKillswitch();
            CwAnalytics.sendAppOpenEvent();
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
        analyticTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                numActivities--;
                if(numActivities == 0) {
                    CwAnalytics.sendAppBackgroundEvent();
                }
            }
        }, 500);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    private void checkKillswitch() {
        KillswitchJob.post();
    }
}
