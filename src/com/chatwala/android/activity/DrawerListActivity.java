package com.chatwala.android.activity;

import android.app.LoaderManager;
import android.content.Loader;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ViewSwitcher;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.ChatwalaNotificationManager;
import com.chatwala.android.R;
import com.chatwala.android.adapters.MessageDrawerAdapter;
import com.chatwala.android.adapters.UserDrawerAdapter;
import com.chatwala.android.database.DrawerMessage;
import com.chatwala.android.database.DrawerUser;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.loaders.MessageLoader;
import com.chatwala.android.loaders.UserLoader;
import com.chatwala.android.superbus.server20.GetUserInboxCommand;
import com.chatwala.android.util.CWAnalytics;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eliezer on 3/25/2014.
 */
public abstract class DrawerListActivity extends BaseChatWalaActivity {
    private static final int LOAD_USERS_REQUEST_CODE = 0;
    private static final int LOAD_MESSAGES_REQUEST_CODE = 1;

    private DrawerLayout drawerLayout;
    private LinearLayout navigationDrawer;
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

    private Picasso picLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.nav_drawer_layout);
        navigationDrawer = (LinearLayout)findViewById(R.id.navigation_drawer);

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
                           picLoader.load(R.drawable.drawer_closed).noFade().into(drawerToggleButton);
                           drawerClosedPicLoaded = true;
                       }
                       else if(slideOffset >= toggleCollisionOffset && drawerClosedPicLoaded) {
                           picLoader.load(R.drawable.drawer_open).noFade().into(drawerToggleButton);
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
                CWAnalytics.sendDrawerOpened();

                ChatwalaNotificationManager.removeNewMessagesNotification(getApplicationContext());
                DataProcessor.runProcess(new Runnable() {
                    @Override
                    public void run() {
                        BusHelper.submitCommandSync(getApplicationContext(), new GetUserInboxCommand());
                    }
                });

                loadUsers();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                CWAnalytics.sendDrawerClosed();
            }
        });

        mainContentFrame = (FrameLayout)findViewById(R.id.main_container);

        picLoader = new Picasso.Builder(getApplicationContext()).build();
        userAdapter = new UserDrawerAdapter(getApplicationContext(), new ArrayList<DrawerUser>(0), picLoader);
        messageAdapter = new MessageDrawerAdapter(getApplicationContext(), new ArrayList<DrawerMessage>(0), picLoader);

        drawerListSwitcher = (ViewSwitcher) findViewById(R.id.drawer_list_switcher);
        drawerListSwitcher.setDisplayedChild(1);

        usersListView = (ListView) findViewById(R.id.users_list);
        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                DrawerUser user = getUserAdapter().getItem(i);
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
                DrawerMessage message = getMessageAdapter().getItem(i);
                NewCameraActivity.startMeWithId(DrawerListActivity.this, message.getReadUrl(), message.getMessageId());
                finish();
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
                slideLists();
            }
        });

        settingsButton = (ImageView)findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsActivity.startMe(DrawerListActivity.this);
            }
        });
    }

    private void slideLists() {
        if(drawerListSwitcher.getCurrentView() == null) {
            return;
        }

        if(drawerListSwitcher.getCurrentView().findViewById(R.id.users_list) != null) {
            TranslateAnimation out = new TranslateAnimation(0, drawerListSwitcher.getWidth(), 0, 0);
            out.setInterpolator(new AccelerateInterpolator());
            out.setDuration(400);
            TranslateAnimation in = new TranslateAnimation(-drawerListSwitcher.getWidth(), 0, 0, 0);
            in.setInterpolator(new AccelerateInterpolator());
            in.setDuration(400);
            drawerListSwitcher.setInAnimation(in);
            drawerListSwitcher.setOutAnimation(out);
            drawerListSwitcher.showPrevious();
        }
        else {
            TranslateAnimation out = new TranslateAnimation(0, -drawerListSwitcher.getWidth(), 0, 0);
            out.setInterpolator(new AccelerateInterpolator());
            out.setDuration(400);
            TranslateAnimation in = new TranslateAnimation(drawerListSwitcher.getWidth(), 0, 0, 0);
            in.setInterpolator(new AccelerateInterpolator());
            in.setDuration(400);
            drawerListSwitcher.setInAnimation(in);
            drawerListSwitcher.setOutAnimation(out);
            drawerListSwitcher.showNext();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        drawerToggleButton.bringToFront();
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

    protected void toggleDrawerEnabled(boolean enabled) {
        if(enabled) {
            drawerToggleButton.setVisibility(View.VISIBLE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, navigationDrawer);
        }
        else {
            drawerToggleButton.setVisibility(View.GONE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navigationDrawer);
        }
    }

    protected abstract void performAddButtonAction();

    private UserDrawerAdapter getUserAdapter() {
        if(userAdapter == null) {
            userAdapter = new UserDrawerAdapter(getApplicationContext(), new ArrayList<DrawerUser>(0), picLoader);
        }
        return userAdapter;
    }

    private void setUserAdapterList(List<DrawerUser> list) {
        getUserAdapter().useNewUsersList(list);
    }

    private MessageDrawerAdapter getMessageAdapter() {
        if(messageAdapter == null) {
            messageAdapter = new MessageDrawerAdapter(getApplicationContext(), new ArrayList<DrawerMessage>(0), picLoader);
        }
        return messageAdapter;
    }

    private void setMessageAdapterList(List<DrawerMessage> list) {
        getMessageAdapter().useNewMessagesList(list);
    }

    private boolean usersLoaded = false;
    private void loadUsers() {
        if(usersLoaded) {
            getLoaderManager().restartLoader(LOAD_USERS_REQUEST_CODE, null, loadUserCallbacks);
        }
        else {
            usersLoaded = true;
            getLoaderManager().initLoader(LOAD_USERS_REQUEST_CODE, null, loadUserCallbacks);
        }
    }

    private boolean messagesLoaded = false;
    private void loadMessages(String senderId) {
        Bundle args = new Bundle();
        args.putString("senderId", senderId);
        if(messagesLoaded) {
            getLoaderManager().restartLoader(LOAD_MESSAGES_REQUEST_CODE, args, loadMessageCallbacks);
        }
        else {
            messagesLoaded = true;
            getLoaderManager().initLoader(LOAD_MESSAGES_REQUEST_CODE, args, loadMessageCallbacks);
        }
    }

    private LoaderManager.LoaderCallbacks<List<DrawerUser>> loadUserCallbacks = new LoaderManager.LoaderCallbacks<List<DrawerUser>>() {
        @Override
        public Loader<List<DrawerUser>> onCreateLoader(int i, Bundle bundle) {
            return new UserLoader(getApplicationContext());
        }

        @Override
        public void onLoadFinished(Loader<List<DrawerUser>> listLoader, List<DrawerUser> drawerUsers) {
            setUserAdapterList(drawerUsers);
        }

        @Override
        public void onLoaderReset(Loader<List<DrawerUser>> listLoader) {}
    };

    private LoaderManager.LoaderCallbacks<List<DrawerMessage>> loadMessageCallbacks = new LoaderManager.LoaderCallbacks<List<DrawerMessage>>() {
        @Override
        public Loader<List<DrawerMessage>> onCreateLoader(int i, Bundle bundle) {
            return new MessageLoader(getApplicationContext(), bundle.getString("senderId"));
        }

        @Override
        public void onLoadFinished(Loader<List<DrawerMessage>> listLoader, List<DrawerMessage> drawerMessages) {
            setMessageAdapterList(drawerMessages);
        }

        @Override
        public void onLoaderReset(Loader<List<DrawerMessage>> listLoader) {}
    };
}
