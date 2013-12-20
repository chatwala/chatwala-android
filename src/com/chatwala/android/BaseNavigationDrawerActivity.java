package com.chatwala.android;

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
import android.widget.*;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.loaders.MessagesLoader;
import com.chatwala.android.superbus.GetMessagesForUserCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    private final int messagesLoaderId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.nav_drawer_layout);

        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener()
        {
            FrameLayout.LayoutParams toggleButtonParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            @Override
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                drawerToggleButton.setVisibility(View.VISIBLE);
                drawerToggleButton.setImageResource(R.drawable.drawer_open);
                Resources r = getResources();
                float leftpx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 260, r.getDisplayMetrics());
                float toppx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, r.getDisplayMetrics());
                toggleButtonParams.setMargins((int)leftpx, (int)toppx, 0, 0);
                drawerToggleButton.setLayoutParams(toggleButtonParams);
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
                drawerToggleButton.setVisibility(View.VISIBLE);
                drawerToggleButton.setImageResource(R.drawable.drawer_closed);
                Resources r = getResources();
                float toppx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, r.getDisplayMetrics());
                toggleButtonParams.setMargins((int)toppx, (int)toppx, 0, 0);
                drawerToggleButton.setLayoutParams(toggleButtonParams);
            }
        });

        navigationDrawer = (LinearLayout)findViewById(R.id.navigation_drawer);
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
        getLoaderManager().initLoader(messagesLoaderId, null, new LoaderManager.LoaderCallbacks<List<ChatwalaMessage>>() {
            @Override
            public Loader<List<ChatwalaMessage>> onCreateLoader(int id, Bundle args)
            {
                return new MessagesLoader(BaseNavigationDrawerActivity.this);
            }

            @Override
            public void onLoadFinished(Loader<List<ChatwalaMessage>> loader, List<ChatwalaMessage> data)
            {
                messagesListView.setAdapter(new DrawerMessagesAdapter(data));
            }

            @Override
            public void onLoaderReset(Loader<List<ChatwalaMessage>> loader)
            {
                //Nothing for now.
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

    class DrawerMessagesAdapter extends BaseAdapter
    {
        ArrayList<ChatwalaMessage> messageList;

        DrawerMessagesAdapter(List<ChatwalaMessage> messageList)
        {
            this.messageList = new ArrayList<ChatwalaMessage>(messageList);
            Collections.sort(this.messageList, new Comparator<ChatwalaMessage>()
            {
                @Override
                public int compare(ChatwalaMessage lhs, ChatwalaMessage rhs)
                {
                    return lhs.getSortId() - rhs.getSortId();
                }
            });
        }

        @Override
        public int getCount()
        {
            return messageList.size();
        }

        @Override
        public Object getItem(int position)
        {
            return messageList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if(convertView == null)
            {
                convertView = new TextView(BaseNavigationDrawerActivity.this);
                convertView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        NewCameraActivity.startMeWithId(BaseNavigationDrawerActivity.this, (String)v.getTag());
                        finish();
                    }
                });
            }

            ChatwalaMessage message = (ChatwalaMessage)getItem(position);
            ((TextView)convertView).setText(message.getMessageId());
            convertView.setTag(message.getMessageId());

            return convertView;
        }
    }
}
