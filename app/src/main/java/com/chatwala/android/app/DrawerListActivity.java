package com.chatwala.android.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;
import com.chatwala.android.R;
import com.chatwala.android.events.DrawerUpdateEvent;
import com.chatwala.android.files.FileManager;
import com.chatwala.android.messages.*;
import com.chatwala.android.queue.jobs.GetUserInboxJob;
import com.chatwala.android.util.CwAnalytics;
import de.greenrobot.event.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DrawerListActivity extends BaseChatwalaActivity {
    private static final int LOAD_USERS_REQUEST_CODE = 0;
    private static final int LOAD_MESSAGES_REQUEST_CODE = 1;

    private DrawerLayout drawerLayout;
    private RelativeLayout navigationDrawer;
    private FrameLayout mainContentFrame;
    private ImageView drawerToggleButton;
    private ViewSwitcher drawerListSwitcher;
    private ListView usersListView;
    private ListView messagesListView;

    private UserDrawerAdapter userAdapter;
    private MessageDrawerAdapter messageAdapter;

    private ImageView settingsButton;
    private ImageView addButton;
    private ImageView backButton;
    private ImageView feedbackButton;
    private ImageView newUserMessageButton;

    private enum DrawerState {
        USERS, MESSAGES
    }
    private DrawerState drawerState = DrawerState.USERS;

    private void loadResourceIntoImageView(ImageView iv, int res) {
        iv.setImageResource(res);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.nav_drawer_layout);

        navigationDrawer = (RelativeLayout)findViewById(R.id.navigation_drawer);

        drawerToggleButton = (ImageView)findViewById(R.id.drawer_toggle_button);
        drawerToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(drawerLayout.isDrawerOpen(navigationDrawer)) {
                    closeDrawer();
                }
                else {
                    openDrawer();
                }
            }
        });

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            FrameLayout.LayoutParams toggleButtonLP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            private Resources r = getResources();
            private boolean drawerClosedPicLoaded = false;
            private final float toggleStartingLeftMargin = 20f;
            private final int toggleStaticTopMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, r.getDisplayMetrics());
            private final float toggleCollisionOffset = .17f;
            private final float toggleEndingLeftMargin = 170f;

            @Override
            public void onDrawerSlide(View drawerView, final float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                drawerToggleButton.post(new Runnable() {
                   @Override
                   public void run() {
                       float base = toggleStartingLeftMargin;
                       if(slideOffset >= toggleCollisionOffset) {
                           base = toggleEndingLeftMargin * slideOffset;
                       }

                       if(slideOffset < toggleCollisionOffset && !drawerClosedPicLoaded) {
                           loadResourceIntoImageView(drawerToggleButton, R.drawable.drawer_closed);
                           drawerClosedPicLoaded = true;
                       }
                       else if(slideOffset >= toggleCollisionOffset && drawerClosedPicLoaded) {
                           loadResourceIntoImageView(drawerToggleButton, R.drawable.drawer_open);
                           drawerClosedPicLoaded = false;
                       }

                       float leftpx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, base, r.getDisplayMetrics());
                       toggleButtonLP.setMargins((int)leftpx, toggleStaticTopMargin, 0, 0);
                       drawerToggleButton.setLayoutParams(toggleButtonLP);
                   }
                });
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                CwAnalytics.sendDrawerOpened();

                GetUserInboxJob.post();

                if(drawerState == DrawerState.USERS) {
                    loadUsers();
                }
                else if(drawerState == DrawerState.MESSAGES) {
                    loadMessages(currentSenderId);
                }

                EventBus.getDefault().register(DrawerListActivity.this);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                CwAnalytics.sendDrawerClosed();

                //this may crash if registration did not go through. just be safe
                try {
                    EventBus.getDefault().unregister(DrawerListActivity.this);
                }
                catch (Throwable ignore) {}
            }
        });

        mainContentFrame = (FrameLayout)findViewById(R.id.main_container);

        userAdapter = new UserDrawerAdapter(this, new ArrayList<ChatwalaUser>(0));
        messageAdapter = new MessageDrawerAdapter(this, new ArrayList<ChatwalaMessage>(0));

        drawerListSwitcher = (ViewSwitcher) findViewById(R.id.drawer_list_switcher);

        usersListView = (ListView) findViewById(R.id.users_list);
        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                ChatwalaUser user = getUserAdapter().getItem(i);
                loadMessages(user.getSenderId());
                slideLists();
            }
        });
        usersListView.setAdapter(userAdapter);
        loadUsers();

        messagesListView = (ListView) findViewById(R.id.messages_list);
        messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                ChatwalaMessage message = getMessageAdapter().getItem(i);
                onMessageSelected(message);
            }
        });
        messagesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                showDeleteDialog(i);
                return true;
            }
        });
        messagesListView.setAdapter(messageAdapter);

        addButton = (ImageView)findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performAddButtonAction();
            }
        });

        backButton = (ImageView)findViewById(R.id.drawer_back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUsers();
                getMessageAdapter().clearMessages();
                slideLists();
            }
        });

        settingsButton = (ImageView)findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DrawerListActivity.this, SettingsActivity.class));
            }
        });

        feedbackButton = (ImageView)findViewById(R.id.user_footer);
        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DrawerListActivity.this, FeedbackActivity.class));
            }
        });

        newUserMessageButton = (ImageView) findViewById(R.id.message_footer);
        newUserMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNewUserMessageClicked(currentSenderId);
            }
        });
    }

    private void showDeleteDialog(final int position) {
        DeleteConfirmationDialog.newInstance(position, currentSenderId).show(getSupportFragmentManager(), "dialog");
    }

    private void slideLists() {
        if(drawerListSwitcher.getCurrentView() == null) {
            return;
        }

        if(drawerState == DrawerState.USERS) {
            TranslateAnimation out = new TranslateAnimation(0, -drawerListSwitcher.getWidth(), 0, 0);
            out.setDuration(400);
            TranslateAnimation in = new TranslateAnimation(drawerListSwitcher.getWidth(), 0, 0, 0);
            in.setDuration(400);
            drawerListSwitcher.setInAnimation(in);
            drawerListSwitcher.setOutAnimation(out);
            drawerListSwitcher.showNext();
            setNewUserMessageButtonBackground();
            drawerState = DrawerState.MESSAGES;
        }
        else if(drawerState == DrawerState.MESSAGES) {
            TranslateAnimation out = new TranslateAnimation(0, drawerListSwitcher.getWidth(), 0, 0);
            out.setDuration(400);
            TranslateAnimation in = new TranslateAnimation(-drawerListSwitcher.getWidth(), 0, 0, 0);
            in.setDuration(400);
            drawerListSwitcher.setInAnimation(in);
            drawerListSwitcher.setOutAnimation(out);
            drawerListSwitcher.showPrevious();
            drawerState = DrawerState.USERS;
        }
    }

    private void setNewUserMessageButtonBackground() {
        if(currentSenderId == null) {
            return;
        }
        File userThumb = FileManager.getUserThumb(currentSenderId);
        if(!userThumb.exists()) {
            return;
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            newUserMessageButton.setBackground(Drawable.createFromPath(userThumb.getAbsolutePath()));
        }
        else {
            newUserMessageButton.setBackgroundDrawable(Drawable.createFromPath(userThumb.getAbsolutePath()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        drawerToggleButton.bringToFront();

        if(drawerLayout.isDrawerOpen(navigationDrawer)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(drawerLayout.isDrawerOpen(navigationDrawer)) {
            //this may crash if registration did not go through. just be safe
            try {
                EventBus.getDefault().unregister(this);
            }
            catch (Throwable ignore) {}
        }
    }

    public void onEventMainThread(DrawerUpdateEvent event) {
        if(drawerState == DrawerState.USERS) {
            if(event.isLoadEvent()) {
                loadUsers();
            }
            else {
                getUserAdapter().notifyDataSetChanged();
            }
        }
        else if(drawerState == DrawerState.MESSAGES) {
            if(event.isLoadEvent()) {
                loadMessages(currentSenderId);
            }
            else {
                getMessageAdapter().notifyDataSetChanged();
            }
        }
    }

    protected void setMainContent(View v) {
        mainContentFrame.addView(v);
    }

    protected void closeDrawer() {
        drawerLayout.closeDrawer(navigationDrawer);
    }

    protected void openDrawer() {
        drawerLayout.openDrawer(navigationDrawer);
    }

    public void setBurgerEnabled(boolean burgerEnabled) {
        if(burgerEnabled) {
            drawerToggleButton.setVisibility(View.VISIBLE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, navigationDrawer);
        }
        else {
            drawerToggleButton.setVisibility(View.GONE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navigationDrawer);
        }
    }

    protected abstract void performAddButtonAction();

    protected abstract void onMessageSelected(ChatwalaMessage message);

    protected abstract void onNewUserMessageClicked(String userId);

    private UserDrawerAdapter getUserAdapter() {
        if(userAdapter == null) {
            userAdapter = new UserDrawerAdapter(getApplicationContext(), new ArrayList<ChatwalaUser>(0));
        }
        return userAdapter;
    }

    private void setUserAdapterList(List<ChatwalaUser> list) {
        getUserAdapter().useNewUsersList(list);
    }

    private MessageDrawerAdapter getMessageAdapter() {
        if(messageAdapter == null) {
            messageAdapter = new MessageDrawerAdapter(getApplicationContext(), new ArrayList<ChatwalaMessage>(0));
        }
        return messageAdapter;
    }

    private void setMessageAdapterList(List<ChatwalaMessage> list) {
        getMessageAdapter().useNewMessagesList(list);
    }

    private boolean usersLoaded = false;
    private void loadUsers() {
        currentSenderId = null;
        if(usersLoaded) {
            getSupportLoaderManager().restartLoader(LOAD_USERS_REQUEST_CODE, null, loadUserCallbacks);
        }
        else {
            usersLoaded = true;
            getSupportLoaderManager().initLoader(LOAD_USERS_REQUEST_CODE, null, loadUserCallbacks);
        }
    }

    private String currentSenderId;
    private boolean messagesLoaded = false;
    private void loadMessages() {
        if(currentSenderId != null) {
            loadMessages(currentSenderId);
        }
    }

    private void loadMessages(String senderId) {
        Bundle args = new Bundle();
        args.putString("senderId", senderId);
        currentSenderId = senderId;
        if(messagesLoaded) {
            getSupportLoaderManager().restartLoader(LOAD_MESSAGES_REQUEST_CODE, args, loadMessageCallbacks);
        }
        else {
            messagesLoaded = true;
            getSupportLoaderManager().initLoader(LOAD_MESSAGES_REQUEST_CODE, args, loadMessageCallbacks);
        }
    }

    private LoaderManager.LoaderCallbacks<List<ChatwalaUser>> loadUserCallbacks = new LoaderManager.LoaderCallbacks<List<ChatwalaUser>>() {
        @Override
        public Loader<List<ChatwalaUser>> onCreateLoader(int i, Bundle bundle) {
            return new UserLoader(getApplicationContext());
        }

        @Override
        public void onLoadFinished(Loader<List<ChatwalaUser>> listLoader, List<ChatwalaUser> ChatwalaUsers) {
            setUserAdapterList(ChatwalaUsers);
        }

        @Override
        public void onLoaderReset(Loader<List<ChatwalaUser>> listLoader) {}
    };

    private LoaderManager.LoaderCallbacks<List<ChatwalaMessage>> loadMessageCallbacks = new LoaderManager.LoaderCallbacks<List<ChatwalaMessage>>() {
        @Override
        public Loader<List<ChatwalaMessage>> onCreateLoader(int i, Bundle bundle) {
            return new MessageLoader(getApplicationContext(), bundle.getString("senderId"));
        }

        @Override
        public void onLoadFinished(Loader<List<ChatwalaMessage>> listLoader, List<ChatwalaMessage> messages) {
            setMessageAdapterList(messages);
        }

        @Override
        public void onLoaderReset(Loader<List<ChatwalaMessage>> listLoader) {}
    };

    public static class DeleteConfirmationDialog extends DialogFragment {
        public DeleteConfirmationDialog() {}

        private int positionToDelete;
        private String currentSenderId;

        public static DeleteConfirmationDialog newInstance(int position, String currentSenderId) {
            DeleteConfirmationDialog frag = new DeleteConfirmationDialog();
            Bundle args = new Bundle();
            args.putInt("position", position);
            args.putString("currentSenderId", currentSenderId);
            frag.setArguments(args);
            return frag;
        }

        private DrawerListActivity activity;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            try {
                this.activity = (DrawerListActivity) activity;
            }
            catch(Exception ignore) {}

            positionToDelete = getArguments().getInt("position");
            currentSenderId = getArguments().getString("currentSendId");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            Dialog d;

            builder.setTitle("Are you sure you would like to delete this message?")
                    .setCancelable(false)
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (activity != null) {
                                MessageManager.delete(activity.getMessageAdapter().getItem(positionToDelete));
                                activity.getMessageAdapter().remove(positionToDelete);
                                if (activity.getMessageAdapter().getCount() == 0) {
                                    activity.getUserAdapter().removeBySenderId(currentSenderId);
                                    activity.slideLists();
                                }
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
            setCancelable(false);
            d = builder.create();
            return d;
        }
    }
}
