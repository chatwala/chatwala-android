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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.ChatwalaNotificationManager;
import com.chatwala.android.R;
import com.chatwala.android.adapters.DrawerMessagesAdapter;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DrawerMessageWrapper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.loaders.MessagesLoader;
import com.chatwala.android.superbus.GetMessagesForUserCommand;
import com.chatwala.android.util.CWAnalytics;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/16/13
 * Time: 5:48 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseNavigationDrawerActivity extends BaseChatWalaActivity
{
    private DrawerLayout drawerLayout;
    private LinearLayout navigationDrawer;
    private FrameLayout mainContentFrame;
    private ImageView drawerToggleButton;
    private ListView messagesListView;

    private DrawerMessagesAdapter drawerMessagesAdapter;
    private List<DrawerMessageWrapper> topLevelMessageList = null;

    private ImageView settingsButton, addButton, backButton;

    private final int messagesLoaderId = 0;

    private Picasso imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.nav_drawer_layout);

        imageLoader = new Picasso.Builder(BaseNavigationDrawerActivity.this).build();

        navigationDrawer = (LinearLayout)findViewById(R.id.navigation_drawer);
//        Display display = getWindowManager().getDefaultDisplay();
//        Point screenSize = new Point();
//        display.getSize(screenSize);
//
//        DisplayMetrics metrics = getResources().getDisplayMetrics();
//        float dpWidth = ( screenSize.x / (metrics.densityDpi / 160f) )/3;
//
//        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams)navigationDrawer.getLayoutParams();
//        params.width = (int)dpWidth;
//        navigationDrawer.setLayoutParams(params);

        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener()
        {
            FrameLayout.LayoutParams toggleButtonParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            @Override
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                CWAnalytics.sendDrawerOpened();
                drawerToggleButton.setVisibility(View.VISIBLE);
                drawerToggleButton.setImageResource(R.drawable.drawer_open);
                Resources r = getResources();
                float leftpx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, r.getDisplayMetrics());
                float toppx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, r.getDisplayMetrics());
                toggleButtonParams.setMargins((int)leftpx, (int)toppx, 0, 0);
                drawerToggleButton.setLayoutParams(toggleButtonParams);

                ChatwalaNotificationManager.removeNewMessagesNotification(BaseNavigationDrawerActivity.this);
                DataProcessor.runProcess(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        BusHelper.submitCommandSync(BaseNavigationDrawerActivity.this, new GetMessagesForUserCommand());
                    }
                });
            }

            @Override
            public void onDrawerClosed(View drawerView)
            {
                super.onDrawerClosed(drawerView);
                CWAnalytics.sendDrawerClosed();
                drawerToggleButton.setVisibility(View.VISIBLE);
                drawerToggleButton.setImageResource(R.drawable.drawer_closed);
                Resources r = getResources();
                float toppx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, r.getDisplayMetrics());
                toggleButtonParams.setMargins((int)toppx, (int)toppx, 0, 0);
                drawerToggleButton.setLayoutParams(toggleButtonParams);
            }
        });

        mainContentFrame = (FrameLayout)findViewById(R.id.main_container);
        drawerToggleButton = (ImageView)findViewById(R.id.drawer_toggle_button);
        drawerToggleButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(drawerLayout.isDrawerOpen(navigationDrawer))
                {
                    closeDrawer();
                }
                else
                {
                    openDrawer();
                }
            }
        });

        messagesListView = (ListView)findViewById(R.id.conversation_list);
        drawerMessagesAdapter = new DrawerMessagesAdapter(BaseNavigationDrawerActivity.this, imageLoader);
        getLoaderManager().initLoader(messagesLoaderId, null, new LoaderManager.LoaderCallbacks<List<ChatwalaMessage>>() {
            @Override
            public Loader<List<ChatwalaMessage>> onCreateLoader(int id, Bundle args)
            {
                return new MessagesLoader(BaseNavigationDrawerActivity.this);
            }

            @Override
            public void onLoadFinished(Loader<List<ChatwalaMessage>> loader, List<ChatwalaMessage> data)
            {
                boolean first = topLevelMessageList == null;
                topLevelMessageList = makeWrappersFromLoaderData(data);
                if(first)
                {
                    messagesListView.setAdapter(drawerMessagesAdapter);
                }

                if(drawerMessagesAdapter.getCurrentSenderId() == null)
                {
                    setDefaultAdapterData();
                }
                else
                {
                    String currentDataSender = drawerMessagesAdapter.getCurrentSenderId();
                    for(DrawerMessageWrapper message : topLevelMessageList)
                    {
                        if(message.getSenderId().equals(currentDataSender))
                        {
                            setAdapterData(message.getMessageWrapperList());
                            break;
                        }
                    }
                }
            }

            @Override
            public void onLoaderReset(Loader<List<ChatwalaMessage>> loader)
            {
                //Nothing for now.
            }
        });

        addButton = (ImageView)findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                performAddButtonAction();
            }
        });

        backButton = (ImageView)findViewById(R.id.drawer_back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDefaultAdapterData();
            }
        });

        settingsButton = (ImageView)findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SettingsActivity.startMe(BaseNavigationDrawerActivity.this);
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        drawerToggleButton.bringToFront();
    }

    protected void setMainContent(View v)
    {
        mainContentFrame.addView(v);
    }

    protected void closeDrawer()
    {
        drawerLayout.closeDrawer(navigationDrawer);
    }

    protected void openDrawer()
    {
        drawerLayout.openDrawer(navigationDrawer);
    }

    protected void toggleDrawerEnabled(boolean enabled)
    {
        if(enabled)
        {
            drawerToggleButton.setVisibility(View.VISIBLE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, navigationDrawer);
        }
        else
        {
            drawerToggleButton.setVisibility(View.GONE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navigationDrawer);
        }
    }

    protected abstract void performAddButtonAction();

    private void setDefaultAdapterData()
    {
        backButton.setVisibility(View.GONE);
        addButton.setVisibility(View.VISIBLE);
        drawerMessagesAdapter.swapData(topLevelMessageList, null);
    }

    public void setAdapterData(List<DrawerMessageWrapper> incomingList)
    {
        backButton.setVisibility(View.VISIBLE);
        addButton.setVisibility(View.GONE);
        drawerMessagesAdapter.swapData(incomingList, incomingList.get(0).getSenderId());
    }

    private List<DrawerMessageWrapper> makeWrappersFromLoaderData(List<ChatwalaMessage> messageList)
    {
        ArrayList<DrawerMessageWrapper> messageWrapperList = new ArrayList<DrawerMessageWrapper>();

        for(ChatwalaMessage message :messageList)
        {
            messageWrapperList.add(new DrawerMessageWrapper(message));
        }

        return messageWrapperList;

//        HashMap<String, ArrayList<ChatwalaMessage>> messageListMap = new HashMap<String, ArrayList<ChatwalaMessage>>();
//
//        for(ChatwalaMessage message : messageList)
//        {
//            if(messageListMap.containsKey(message.getSenderId()))
//            {
//                messageListMap.get(message.getSenderId()).add(message);
//            }
//            else
//            {
//                ArrayList<ChatwalaMessage> newList = new ArrayList<ChatwalaMessage>();
//                newList.add(message);
//                messageListMap.put(message.getSenderId(), newList);
//            }
//        }
//
//        ArrayList<DrawerMessageWrapper> drawerMessageWrapperList = new ArrayList<DrawerMessageWrapper>();
//        for(ArrayList<ChatwalaMessage> messageListFromMap : messageListMap.values())
//        {
//            drawerMessageWrapperList.add(new DrawerMessageWrapper(messageListFromMap));
//        }
//
//        return drawerMessageWrapperList;
    }
}
