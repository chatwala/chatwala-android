package com.chatwala.android;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Loader;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.loaders.MessagesLoader;
import com.chatwala.android.superbus.GetMessagesForUserCommand;
import com.chatwala.android.superbus.GetUserProfilePictureCommand;
import com.chatwala.android.util.MessageDataStore;
import com.squareup.picasso.Picasso;

import java.io.File;
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

    private ImageView settingsButton, addButton;

    private final int messagesLoaderId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.nav_drawer_layout);

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

        addButton = (ImageView)findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                performAddButtonAction();
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
                convertView = getLayoutInflater().inflate(R.layout.row_drawer_thumb, parent, false);
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

            final ChatwalaMessage message = (ChatwalaMessage)getItem(position);

            File thumbImage = MessageDataStore.findUserImageInLocalStore(message.getSenderId());
            if(thumbImage.exists())
            {
                Picasso.with(BaseNavigationDrawerActivity.this).load(thumbImage).fit().into((ImageView) convertView.findViewById(R.id.thumb_view));
            }
            else
            {
                Picasso.with(BaseNavigationDrawerActivity.this).load(message.getThumbnailUrl()).fit().into((ImageView) convertView.findViewById(R.id.thumb_view));
                DataProcessor.runProcess(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        BusHelper.submitCommandSync(BaseNavigationDrawerActivity.this, new GetUserProfilePictureCommand(message.getSenderId()));
                    }
                });

            }

            ((TextView)convertView.findViewById(R.id.time_since_text)).setText(formatMessageTimestamp(message.getTimestamp()));

            ImageView stateView = (ImageView)convertView.findViewById(R.id.status_image);
            switch(message.getMessageState())
            {
                case UNREAD:
                    stateView.setImageResource(R.drawable.unread_icon);
                    break;
                case REPLIED:
                    stateView.setImageResource(R.drawable.replied_icon);
                    break;
                default:
                    stateView.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            convertView.setTag(message.getMessageId());

            return convertView;
        }
    }

    //This is crude for now while it needs to be done manually, should at least break it into its own class and use some meaningfully named constant variables
    //Todo: revisit when we see what's coming back from the server for this field
    private String formatMessageTimestamp(long messageTimestamp)
    {
        int secondsSince = (int)(System.currentTimeMillis() - messageTimestamp) / 1000;

        if(secondsSince < 60)
        {
            return secondsSince + "s";
        }
        else if(secondsSince < (60*60))
        {
            return (secondsSince/60) + "m";
        }
        else if(secondsSince < (60*60*60))
        {
            return ((secondsSince/60)/60) + "h";
        }
        else if(secondsSince < (60*60*60*24))
        {
            int daysSince = (((secondsSince/60)/60)/24);
            return  daysSince + daysSince == 1 ? " day" : " days";
        }
        else
        {
            int weeksSince = ((((secondsSince/60)/60)/24)/7);
            return  weeksSince + weeksSince == 1 ? " week" : " weeks";
        }
    }

    protected abstract void performAddButtonAction();
}
