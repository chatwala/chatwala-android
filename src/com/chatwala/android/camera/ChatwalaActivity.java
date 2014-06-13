package com.chatwala.android.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import com.chatwala.android.R;
import com.chatwala.android.app.AppPrefs;
import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.app.DrawerListActivity;
import com.chatwala.android.app.KillswitchActivity;
import com.chatwala.android.contacts.TopContactsActivity;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.ChatwalaMessageThreadConversation;
import com.chatwala.android.messages.MessageStartInfo;
import com.chatwala.android.messages.MessageState;
import com.chatwala.android.migration.UpdateManager;
import com.chatwala.android.sms.Sms;
import com.chatwala.android.sms.SmsActivity;
import com.chatwala.android.sms.SmsManager;
import com.chatwala.android.ui.CwButton;
import com.chatwala.android.ui.MessageLoadingTimerFragment;
import com.chatwala.android.ui.PacmanView;
import com.chatwala.android.util.AndroidUtils;
import com.chatwala.android.util.CwAnalytics;
import com.chatwala.android.util.DeliveryMethod;
import com.chatwala.android.util.KillswitchInfo;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.Referrer;
import com.espian.showcaseview.ShowcaseView;
import com.espian.showcaseview.targets.ViewTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChatwalaActivity extends DrawerListActivity {
    private static final int IMMERSIVE_TIMEOUT = 3000;
    private static final String SAVED_PREVIEW_FILE = "SAVED_PREVIEW_FILE";
    private static final String SAVED_PREVIEW_TYPE = "SAVED_PREVIEW_TYPE";

    private static final long RECORDING_START_TIME_INIT = -1;

    //TODO wtf
    private static final String HANGOUTS_PACKAGE_NAME = "com.google.android.talk";

    public static final String OPEN_DRAWER_EXTRA = "OPEN_DRAWER";

    private Handler postMan;
    private ProgressDialog updatingProgressDialog;

    private ChatwalaFragment currentFragment;
    private View messageLoadingTimerContainer;
    private MessageLoadingTimerFragment messageLoadingTimer;
    private PacmanView pacman;
    private CwCamera camera;
    private AcquireCameraAsyncTask acquireCameraTask;
    private CwButton actionButton;
    private boolean actionButtonEnabled = true;
    private DeliveryMethod deliveryMethod;

    private ShowcaseView tutorialView;
    private static final int FIRST_BUTTON_TUTORIAL_ID = 1000;

    private ArrayList<String> topContactsList;
    private boolean wasFirstButtonPressed = true;

    private long recordingStartTime = RECORDING_START_TIME_INIT;

    private OnRecordingFinishedListener recordingFinishedListener;

    private Runnable reenableActionButton = new Runnable() {
        @Override
        public void run() {
            actionButtonEnabled = true;
        }
    };

    private View.OnClickListener onActionClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(tutorialView != null) {
                ShowcaseView.registerShot(ChatwalaActivity.this, tutorialView.getConfigOptions().showcaseId);
                tutorialView.hide();
            }
            if(!wasFirstButtonPressed) {
                wasFirstButtonPressed = true;
                AppPrefs.setFirstButtonPressed();
                setBurgerEnabled(true);
            }
            if(actionButtonEnabled) {
                currentFragment.onActionButtonClicked();
            }
        }
    };

    /*package*/ interface OnRecordingFinishedListener {
        public void onRecordingFinished(RecordingInfo recordingInfo);
    }

    /*IMMERSIVE MODE
    private Runnable systemUiChangeRunnable = new Runnable() {
        @Override
        public void run() {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE);
            }
        }
    };*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMainContent(getLayoutInflater().inflate(R.layout.chatwala_activity, (ViewGroup) getWindow().getDecorView(), false));

        postMan = new Handler(Looper.getMainLooper());

        if(checkKillswitch()) {
            return;
        }

        /*IMMERSIVE MODE
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE);
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int i) {
                    Fragment dialog = getSupportFragmentManager().findFragmentByTag("dialog");
                    if(dialog != null && dialog.isVisible()) {
                        systemUiChangeRunnable.run();
                    }
                    else {
                        getWindow().getDecorView().removeCallbacks(systemUiChangeRunnable);
                        getWindow().getDecorView().postDelayed(systemUiChangeRunnable, IMMERSIVE_TIMEOUT);
                    }
                }
            });
        }*/

        messageLoadingTimerContainer = findViewById(R.id.message_loading_timer_container);

        actionButton = (CwButton) findViewById(R.id.chatwala_button);
        actionButton.setOnClickListener(onActionClick);
        actionButton.bringToFront();
    }

    private void checkIfUpdateDone() {
        if(UpdateManager.isUpdating()) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkIfUpdateDone();
                }
            }, 1000);
        }
        else {
            if(updatingProgressDialog != null) {
                try {
                    updatingProgressDialog.dismiss();
                } catch(Exception ignore) {}
            }
        }
    }

    /*IMMERSIVE MODE
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().removeCallbacks(systemUiChangeRunnable);
            getWindow().getDecorView().postDelayed(systemUiChangeRunnable, IMMERSIVE_TIMEOUT);
        }
    }*/

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(checkKillswitch()) {
            return;
        }

        findReferrer();

        if(UpdateManager.isUpdating()) {
            updatingProgressDialog = ProgressDialog.show(this, "Updating", "Chatwala is updating.\nPlease wait...", true, false);
        }
        checkIfUpdateDone();

        deliveryMethod = AppPrefs.getDeliveryMethod();

        if(shouldGoToTopContactsFlow()) {
            startActivity(new Intent(this, TopContactsActivity.class));
            finish();
            return;
        }
        else {
            handleTopContactsFlowReturn();
        }

        if(!AppPrefs.wasFirstButtonPressed()) {
            setBurgerEnabled(false);
            wasFirstButtonPressed = false;
        }

        showTutorialIfNeeded();

        if(acquireCameraTask == null && camera == null) {
            loadCamera();
        }

        String shareId = getShareIdFromIntent(getIntent());
        if(shareId == null) {
            if(isFinishing()) {
                return;
            }
            else {
                showConversationStarter();
            }
        }
        else {
            showShareIdLoader(shareId);
        }

        if(getIntent().hasExtra(OPEN_DRAWER_EXTRA)) {
            getIntent().removeExtra(OPEN_DRAWER_EXTRA);
            openDrawer();
        }
    }

    private boolean checkKillswitch() {
        KillswitchInfo killswitch = AppPrefs.getKillswitch();
        if(killswitch.isActive()) {
            Intent killswitchIntent = new Intent(this, KillswitchActivity.class);
            killswitchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(killswitchIntent);
            finish();
            return true;
        }
        else {
            return false;
        }
    }

    private String getShareIdFromIntent(Intent intent) {
        String firstLink = AppPrefs.getFirstLink();
        if(intent == null || intent.getData() == null) {
            if(firstLink == null) {
                return null;
            }
            else {
                AppPrefs.setFirstLink(null);
                return firstLink;
            }
        }

        Uri shareUrl = intent.getData();
        intent.setData(null);
        if(shareUrl.getPath() == null) {
            return null;
        }

        Pattern p = Pattern.compile("^(http|https)://(www\\.|)chatwala.com/(dev/|qa/|)\\?.*$");
        String shareId = shareUrl.getQuery() == null ? "" : shareUrl.getQuery();
        Matcher m = p.matcher(shareUrl.toString());

        if(!shareId.isEmpty() && m.matches()) {
            return shareId;
        }
        else if(shareUrl.getHost() != null && shareUrl.getHost().contains("chatwala.com")) {
            Intent browser = new Intent(Intent.ACTION_VIEW);
            browser.addCategory(Intent.CATEGORY_BROWSABLE);
            browser.setData(shareUrl);

            startActivity(AndroidUtils.getChooserIntentExcludingPackage(this, browser, "com.chatwala"));
            return null;
        }
        else {
            return null;
        }
    }

    private void findReferrer() {
        Referrer referrer = null;
        String referrerPref = AppPrefs.getReferrer();
        if(referrerPref != null) {
            referrer = new Referrer(referrerPref);
        }
        else if(getIntent().getData() != null) {
            referrer = new Referrer(getIntent().getData());
            if(referrer.isValid()) {
                getIntent().setData(null);
            }
        }

        if(referrer != null && referrer.isValid()) {
            CwAnalytics.sendReferrerReceivedEvent(referrer);
            /*if(referrer.isFacebookReferrer()) {
                isFacebookFlow = true;
            }
            else if(referrer.isMessageReferrer()) {
                getIntent().putExtra(MESSAGE_ID, referrer.getValue());
            }
            else if(referrer.isCopyReferrer()) {
                recordCopyOverride = referrer.getValue();
            }*/
        }
    }

    //go to top contacts flow if we return true
    private boolean shouldGoToTopContactsFlow() {
        //if either of these extras are present it means we're coming from top contacts...don't wanna go there again
        if(!getIntent().hasExtra(TopContactsActivity.TOP_CONTACTS_LIST_EXTRA) &&
                !getIntent().hasExtra(TopContactsActivity.TOP_CONTACTS_SKIP_EXTRA)) {
            //we came in by clicking a link (or scanning an sms)...don't go to top contacts
            if(getIntent().getData() != null || AppPrefs.getFirstLink() != null) {
                //if the first open was through clicking a link, never go into top contacts flow
                if(!AppPrefs.wasTopContactsShown()) {
                    AppPrefs.setTopContactsShown(true);
                }
                return false;
            }
            if(!AppPrefs.wasTopContactsShown()) {
                AppPrefs.setTopContactsShown(true);
                return true;
            }
            //if top contacts is our delivery method and we didn't come in from a link
            if(isTopContactsDeliveryMethod() && getIntent().getData() == null) {
                //if we didn't come in from clicking on a notification
                if(!getIntent().hasExtra(OPEN_DRAWER_EXTRA)) {
                    return true;
                }
            }
        }
        //catch all for anything that doesn't return true
        return false;
    }

    private void handleTopContactsFlowReturn() {
        //if we came from top contacts with a result
        if(getIntent().hasExtra(TopContactsActivity.TOP_CONTACTS_LIST_EXTRA)) {
            ArrayList<String> topContacts = getIntent().getStringArrayListExtra(TopContactsActivity.TOP_CONTACTS_LIST_EXTRA);
            if(topContacts != null && topContacts.size() > 0) {
                getIntent().removeExtra(TopContactsActivity.TOP_CONTACTS_LIST_EXTRA);
                deliveryMethod = DeliveryMethod.TOP_CONTACTS;
                topContactsList = topContacts;
            }
        }
        //we didn't get a result from top contacts so use the regular delivery method
        else if(isTopContactsDeliveryMethod()) {
            deliveryMethod = AppPrefs.getDeliveryMethod();
            //if the regular delivery method is top contacts, default to chatwala sms
            if(isTopContactsDeliveryMethod()) {
                deliveryMethod = DeliveryMethod.CWSMS;
            }
        }
    }

    private void showTutorialIfNeeded() {
        if(tutorialView != null) {
            tutorialView.hide();
        }

        //if we were opened with any kind of link or we're in top contacts flow don't show the tutorial
        if(getIntent().getData() != null || isTopContactsDeliveryMethod() || AppPrefs.getFirstLink() != null) {
            ShowcaseView.registerShot(this, FIRST_BUTTON_TUTORIAL_ID);
            return;
        }

        ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
        co.showcaseId = FIRST_BUTTON_TUTORIAL_ID;
        co.hideOnClickOutside = false;
        co.shotType = ShowcaseView.TYPE_ONE_SHOT;

        ViewTarget target = new ViewTarget(R.id.chatwala_button, this);
        tutorialView = ShowcaseView.insertShowcaseView(target, this, getString(R.string.tutorial_title), getString(R.string.tutorial_detail), co);
    }

    @Override
    public void onBackPressed() {
        if(currentFragment != null && !currentFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Logger.i("Pausing...");


        CwAnalytics.Initiator initiator;
        if(wasBackButtonPressed()) {
            initiator = CwAnalytics.Initiator.BACK;
        }
        else {
            initiator = CwAnalytics.Initiator.ENVIRONMENT;
        }
        if(isRecording()) {
            CwAnalytics.sendBackgroundWhileRecordingEvent(initiator, getCurrentRecordingDuration());
        }

        if(acquireCameraTask != null) {
            acquireCameraTask.cancel(true);
        }
        if(pacman != null) {
            pacman.stopAndRemove();
            pacman = null;
        }
        if(camera != null) {
            camera.release();
            camera = null;
        }
    }

    public ChatwalaApplication getApp() {
        return ((ChatwalaApplication) getApplication());
    }

    private void loadCamera() {
        if(this.camera != null && !this.camera.hasError()) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                acquireCameraTask = new AcquireCameraAsyncTask(new AcquireCameraAsyncTask.OnCameraReadyListener() {
                    @Override
                    public void onCameraReady(CwCamera camera) {
                        acquireCameraTask = null;

                        if(camera.hasError()) {
                            //try again?
                            loadCamera();
                            Logger.e("Camera loaded with error");
                            return;
                        }

                        ChatwalaActivity.this.camera = camera;
                        Logger.i("Camera loaded");
                    }
                }, dm.widthPixels, dm.heightPixels / 2);
                acquireCameraTask.execute();
                Logger.i("Loading camera");
            }
        });
    }

    private void swapFragment(ChatwalaFragment newFragment, String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft = ft.replace(R.id.chatwala_fragment_container, newFragment, tag);
        ft.commit();
        currentFragment = newFragment;
    }

    public void showConversationStarter() {
        try {
            hideMessageLoadingTimer();
            swapFragment(new ConversationStarterFragment(), "conversation_starter");
            if(camera == null) {
                loadCamera();
            }
            setBurgerEnabled(wasFirstButtonPressed);
        }
        catch(Exception e) {
            Logger.e("There was an error showing the conversation starter", e);
        }
    }

    public void showConversationStarterKnownRecipient(String recipient) {
        try {
            hideMessageLoadingTimer();
            swapFragment(ConversationStarterKnownRecipientFragment.newInstance(recipient), "conversation_starter_known_recipient");
            if(camera == null) {
                loadCamera();
            }
            setBurgerEnabled(wasFirstButtonPressed);
        }
        catch(Exception e) {
            Logger.e("There was an error showing the conversation starter", e);
        }
    }

    public void showConversationReplier(ChatwalaMessage message) {
        try {
            swapFragment(ConversationReplierFragment.newInstance(message), "conversation_replier");
            if(camera == null) {
                loadCamera();
            }
        }
        catch(Exception e) {
            Logger.e("There was an error showing the conversation replier", e);
        }
    }

    public void showConversationViewer(ChatwalaMessageThreadConversation messages) {
        try {
            swapFragment(ConversationViewerFragment.newInstance(messages), "conversation_viewer");
        }
        catch(Exception e) {
            Logger.e("There was an error showing the conversation replier", e);
        }
    }

    public void showPreviewForStarter(VideoMetadata metadata, MessageStartInfo info) {
        try {
            swapFragment(PreviewFragment.newInstance(metadata, info), "starter_preview");
            setBurgerEnabled(false);
        }
        catch(Exception e) {
            Logger.e("There was an error showing the preview for a conversation starter", e);
        }
    }

    public void showPreviewForKnownRecipient(VideoMetadata metadata, MessageStartInfo info, String recipient) {
        try {
            swapFragment(PreviewFragment.newInstance(metadata, info, recipient), "known_recipient_preview");
            setBurgerEnabled(false);
        }
        catch(Exception e) {
            Logger.e("There was an error showing the preview for a conversation starter", e);
        }
    }

    public void showPreviewForReplier(VideoMetadata metadata, ChatwalaMessage replyingToMessage) {
        try {
            swapFragment(PreviewFragment.newInstance(metadata, replyingToMessage), "replier_preview");
            setBurgerEnabled(false);
        }
        catch(Exception e) {
            Logger.e("There was an error showing the preview for a conversation replier", e);
        }
    }

    public void showShareIdLoader(String shareId) {
        try {
            closeDrawer();
            swapFragment(MessageLoaderFragment.newInstance(shareId, true), "share_id_loader");
            setBurgerEnabled(false);
        }
        catch(Exception e) {
            Logger.e("There was an error showing the share id loader", e);
        }
    }

    public void showMessageLoader(ChatwalaMessage message) {
        try {
            closeDrawer();
            swapFragment(MessageLoaderFragment.newInstance(message.getMessageId(), false), "message_loader");
            setBurgerEnabled(false);
        }
        catch(Exception e) {
            Logger.e("There was an error showing the share id loader", e);
        }
    }

    public void showViewerLoader(ChatwalaMessage message) {
        try {
            closeDrawer();
            swapFragment(ConversationLoaderFragment.newInstance(message), "conversation_loader");
            setBurgerEnabled(false);
        }
        catch(Exception e) {
            Logger.e("There was an error showing the share id loader", e);
        }
    }

    public void sendShareUrl(MessageStartInfo info) {
        if(isChatwalaSmsDeliveryMethod()) {
            sendChatwalaSms(info);
        }
        else if(isSmsDeliveryMethod()) {
            sendSms(info);
        }
        else if(isEmailDeliveryMethod()) {
            sendEmail(info);
        }
        else if(isFacebookDeliveryMethod()) {
            sendFacebook(info);
        }
        else if(isTopContactsDeliveryMethod()) {
            sendSmsToTopContacts(info);
        }
    }

    private void sendSmsToTopContacts(MessageStartInfo info) {
        if(topContactsList == null || topContactsList.size() == 0) {
            DeliveryMethod deliveryMethod = AppPrefs.getDeliveryMethod();
            if (deliveryMethod == DeliveryMethod.SMS) {
                sendSms(info);
            }
            else if(deliveryMethod == DeliveryMethod.CWSMS) {
                sendChatwalaSms(info);
            }
            else if(deliveryMethod == DeliveryMethod.EMAIL) {
                sendEmail(info);
            }
            else if(deliveryMethod == DeliveryMethod.FB) {
                sendFacebook(info);
            }
            return;
        }

        String messageLink = info.getShareUrl();
        for (String contact : topContactsList) {
            SmsManager.getInstance().sendSms(new Sms(contact, messageLink, CwAnalytics.getCategory()));
        }
        CwAnalytics.sendTopContactsSentEvent(topContactsList.size());
        if(getIntent().hasExtra(TopContactsActivity.TOP_CONTACTS_SHOW_UPSELL_EXTRA)) {
            getIntent().removeExtra(TopContactsActivity.TOP_CONTACTS_SHOW_UPSELL_EXTRA);
            Intent i = new Intent(this, SmsActivity.class);
            i.putExtra(SmsActivity.SMS_MESSAGE_URL_EXTRA, messageLink);
            i.putExtra(SmsActivity.COMING_FROM_TOP_CONTACTS_EXTRA, TopContactsActivity.INITIAL_TOP_CONTACTS);
            startActivity(i);
            CwAnalytics.sendUpsellShownEvent();
        }
    }

    private void sendChatwalaSms(MessageStartInfo info) {
        String messageLink = info.getShareUrl();
        Intent i = new Intent(this, SmsActivity.class);
        i.putExtra(SmsActivity.SMS_MESSAGE_URL_EXTRA, messageLink);
        startActivity(i);
    }

    //TODO make this normal
    private void sendSms(MessageStartInfo info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(this);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, SmsManager.DEFAULT_MESSAGE + info.getShareUrl());

            //Can be null in case that there is no default, then the user would be able to choose any app that support this intent.
            if (defaultSmsPackageName != null) {
                sendIntent.setPackage(defaultSmsPackageName);
            }
            startActivity(sendIntent);
        }
        else {
            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            sendIntent.setData(Uri.parse("sms:"));
            sendIntent.putExtra("sms_body", SmsManager.DEFAULT_MESSAGE + info.getShareUrl());

            PackageManager pm = getPackageManager();
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(sendIntent, 0);

            ResolveInfo resolveInfo = null;
            if(resolveInfos.size() == 1) {
                resolveInfo = resolveInfos.get(0);
            }
            else if(resolveInfos.size() > 1) {
                for (ResolveInfo ri : resolveInfos) {
                    if(ri.isDefault) {
                        resolveInfo = ri;
                        break;
                    }
                }
                if(resolveInfo == null) {
                    List<ResolveInfo> trimApps = new ArrayList<ResolveInfo>(resolveInfos.size());
                    for (ResolveInfo ri : resolveInfos) {
                        String packageName = ri.activityInfo.applicationInfo.packageName;
                        if(!packageName.equalsIgnoreCase(HANGOUTS_PACKAGE_NAME)) {
                            trimApps.add(ri);
                        }
                    }
                    if(trimApps.size() == 1) {
                        resolveInfo = trimApps.get(0);
                    }
                }
            }

            if(resolveInfo != null) {
                ActivityInfo activity = resolveInfo.activityInfo;
                ComponentName name = new ComponentName(activity.applicationInfo.packageName, activity.name);
                sendIntent.setComponent(name);
            }

            startActivity(sendIntent);
        }
    }

    //TODO make this normal
    private void sendEmail(MessageStartInfo info) {
        String uriText = "mailto:";

        Uri mailtoUri = Uri.parse(uriText);
        //String messageLink = "<a href=\"http://chatwala.com/?" + messageId + "\">View the message</a>.";
        //String messageLink = EnvironmentVariables.get().getWebPath() + messageId;

        boolean gmailOk = false;

        Intent gmailIntent = new Intent();
        gmailIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
        gmailIntent.setData(mailtoUri);
        gmailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.message_subject));
        //gmailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Chatwala is a new way to have real conversations with friends. " + messageLink));
        gmailIntent.putExtra(Intent.EXTRA_TEXT, SmsManager.DEFAULT_MESSAGE + info.getShareUrl());

        try {
            startActivity(gmailIntent);
            gmailOk = true;
        }
        catch (ActivityNotFoundException ex) {
            Logger.e("Couldn't send GMail", ex);
        }

        if (!gmailOk) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);

            intent.setData(mailtoUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.message_subject));
            //intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Chatwala is a new way to have real conversations with friends. " + messageLink));
            intent.putExtra(Intent.EXTRA_TEXT, SmsManager.DEFAULT_MESSAGE + info.getShareUrl());

            startActivity(Intent.createChooser(intent, "Send email..."));
        }
    }

    private void sendFacebook(MessageStartInfo info) {
        String urlToShare = info.getShareUrl();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, urlToShare);

        // See if official Facebook app is found
        boolean facebookAppFound = false;
        List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo ri : matches) {
            if (ri.activityInfo.packageName.toLowerCase().startsWith("com.facebook")) {
                intent.setPackage(ri.activityInfo.packageName);
                facebookAppFound = true;
                break;
            }
        }

        // As fallback, launch sharer.php in a browser
        if (!facebookAppFound) {
            String sharerUrl = "https://www.facebook.com/sharer/sharer.php?u=" + urlToShare;
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sharerUrl));
        }

        startActivity(intent);
        //startActivityForResult(intent, FACEBOOK_DELIVERY_REQUEST_CODE);
    }

    public void showMessageLoadingTimer() {
        messageLoadingTimer = new MessageLoadingTimerFragment();
        messageLoadingTimerContainer.setVisibility(View.VISIBLE);
        messageLoadingTimerContainer.bringToFront();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.message_loading_timer_container, messageLoadingTimer, "message_loading_timer").commit();
    }

    public boolean isMessageLoadingTimerShowing() {
        return messageLoadingTimer != null;
    }

    public void fadeAndHideMessageLoadingTimer() {
        ViewPropertyAnimator animator = messageLoadingTimerContainer.animate();
        if(animator == null) {
            hideMessageLoadingTimer();
            return;
        }
        animator.alpha(0)
                .setDuration(350)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        messageLoadingTimerContainer.setAlpha(1);
                        hideMessageLoadingTimer();
                    }
                })
                .start();
    }

    public void hideMessageLoadingTimer() {
        if(messageLoadingTimer != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(messageLoadingTimer).commit();
            messageLoadingTimer = null;
        }
        messageLoadingTimerContainer.setVisibility(View.GONE);
    }

    public void setMessageLoadingTimerProgress(int progress) {
        if(messageLoadingTimer != null) {
            messageLoadingTimer.setProgress(progress);
        }
    }

    @Override
    protected void performAddButtonAction() {
        closeDrawer();
        if(getSupportFragmentManager().findFragmentByTag("conversation_starter") == null) {
            showConversationStarter();
        }
    }

    @Override
    protected void onMessageSelected(final ChatwalaMessage message) {
        if(message.getMessageState() == MessageState.REPLIED) {
            showViewerLoader(message);
        }
        else {
            if (message.isInLocalStorage()) {
                if (!isMessageLoadingTimerShowing()) {
                    showMessageLoadingTimer();
                }
                setMessageLoadingTimerProgress(99);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showConversationReplier(message);
                    }
                }, 1000);
            } else {
                showMessageLoader(message);
            }
        }
        closeDrawer();
        setBurgerEnabled(false);
    }

    @Override
    protected void onNewUserMessageClicked(String userId) {
        closeDrawer();
        showConversationStarterKnownRecipient(userId);
    }

    public void setPreviewForCamera(final TextureView surface) {
        if(camera == null && surface != null) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    setPreviewForCamera(surface);
                }
            }, 250);
            return;
        }

        if(surface != null && surface.isAvailable()) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (camera != null && !camera.isShowingPreview() && surface != null && surface.isAvailable() &&
                            surface.getSurfaceTexture() != null) {
                        camera.attachToPreview(surface.getSurfaceTexture());
                    }
                }
            }, 250);
        }
    }

    public boolean isShowingCameraPreview() {
        if(camera == null) {
            return false;
        }
        return camera.isShowingPreview();
    }

    public boolean stopCameraPreview() {
        if(camera != null && camera.isShowingPreview()) {
            return camera.stopPreview();
        }
        else {
            return false;
        }
    }

    public boolean toggleCamera() {
        if(camera != null && !camera.hasError()) {
            return camera.toggleCamera();
        }
        return false;
    }

    public boolean isRecording() {
        if(camera == null) {
            return false;
        }
        return camera.isRecording();
    }

    public long getRecordingStartTime() {
        return recordingStartTime;
    }

    public boolean wasFirstButtonPressed() {
        return wasFirstButtonPressed;
    }

    public long getCurrentRecordingDuration() {
        return System.currentTimeMillis() - recordingStartTime;
    }

    public boolean startRecording(long maxRecordingMillis, CwAnalytics.Initiator initiator, String analyticsStartAction,
                                  final String analyticsCompleteAction, final OnRecordingFinishedListener recordingFinishedListener) {
        this.recordingFinishedListener = recordingFinishedListener;
        MediaRecorder.OnInfoListener listener = new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    if(camera != null) {
                        Logger.d("Reached max recording duration and we still have the camera");
                        stopRecording(CwAnalytics.Initiator.AUTO, analyticsCompleteAction, false);
                    }
                    else {
                        if(recordingFinishedListener != null) {
                            recordingFinishedListener.onRecordingFinished(null);
                        }
                        Logger.d("Reached max recording duration and we don't have the camera");
                    }
                }
            }
        };

        if(camera == null) {
            Logger.e("Camera is null so we're gonna try to reload it");
            loadCamera();
            return false;
        }

        if(!camera.canRecord() || !camera.startRecording(maxRecordingMillis, listener)) {
            Logger.e("Couldn't start recording");
            if(camera.hasError()) {
                loadCamera();
                Logger.e("Camera is in an error state so we're gonna try to reload it");
            }
            return false;
        }
        else {
            setBurgerEnabled(false);
            if(pacman != null) {
                pacman.stopAndRemove();
            }
            pacman = new PacmanView(this);
            addActionView(pacman, 0);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            pacman.setLayoutParams(lp);
            pacman.startAnimation(maxRecordingMillis);
            actionButton.setOnClickListener(null);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                   actionButton.setOnClickListener(onActionClick);
                }
            }, 1000);
            recordingStartTime = System.currentTimeMillis();

            CwAnalytics.sendRecordingStartEvent(initiator, analyticsStartAction);
            return true;
        }
    }

    public boolean stopRecording() {
        return stopRecording(null, null, true);
    }

    public boolean stopRecording(CwAnalytics.Initiator initiator, String analyticsAction) {
        return stopRecording(initiator, analyticsAction, true);
    }

    private boolean stopRecording(CwAnalytics.Initiator initiator, String analyticsAction, boolean actuallyStop) {
        setBurgerEnabled(true);
        recordingStartTime = RECORDING_START_TIME_INIT;
        if(camera != null) {
            if(pacman != null) {
                pacman.stopAndRemove();
                pacman = null;
            }
            RecordingInfo recordingInfo = camera.stopRecording(actuallyStop);
            if(recordingFinishedListener != null) {
                recordingFinishedListener.onRecordingFinished(recordingInfo);
            }
            if(initiator != null && analyticsAction != null) {
                CwAnalytics.sendRecordingStopEvent(initiator, analyticsAction, recordingInfo.getRecordingLength());
            }
            return true;
        }
        else {
            return false;
        }
    }

    public void disableActionButton(long disableTimeMillis) {
        actionButton.removeCallbacks(reenableActionButton);
        actionButtonEnabled = false;
        actionButton.postDelayed(reenableActionButton, disableTimeMillis);
    }

    public boolean isChatwalaSmsDeliveryMethod() {
        return deliveryMethod == DeliveryMethod.CWSMS;
    }

    public boolean isSmsDeliveryMethod() {
        return deliveryMethod == DeliveryMethod.SMS;
    }

    public boolean isEmailDeliveryMethod() {
        return deliveryMethod == DeliveryMethod.EMAIL;
    }

    public boolean isFacebookDeliveryMethod() {
        return deliveryMethod == DeliveryMethod.FB;
    }

    public boolean isTopContactsDeliveryMethod() {
        return deliveryMethod == DeliveryMethod.TOP_CONTACTS;
    }

    public void setActionView(View v) {
        actionButton.setActionView(v);
    }

    public void addActionView(View v) {
        actionButton.addActionView(v);
    }

    public void replaceActionViewAt(View v, int position) {
        removeActionViewAt(position);
        addActionView(v, position);
    }

    public void addActionView(View v, int position) {
        actionButton.addActionView(v, position);
    }

    public View getActionViewAt(int position) {
        return actionButton.getActionViewAt(position);
    }

    public void removeActionViewAt(int position) {
        actionButton.removeActionViewAt(position);
    }

    public void clearActionView() {
        actionButton.clearActionView();
    }

    public void post(Runnable r) {
        if(postMan != null) {
            postMan.post(r);
        }
    }

    public void postDelayed(Runnable r, long delay) {
        if(postMan != null) {
            postMan.postDelayed(r, delay);
        }
    }
}
