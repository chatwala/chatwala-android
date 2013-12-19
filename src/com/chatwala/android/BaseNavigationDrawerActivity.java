package com.chatwala.android;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/16/13
 * Time: 5:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class BaseNavigationDrawerActivity extends BaseChatWalaActivity
{
    private DrawerLayout drawerLayout;
    private FrameLayout navigationDrawer;
    private FrameLayout mainContentFrame;
    private ImageView drawerToggleButton;

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

        navigationDrawer = (FrameLayout)findViewById(R.id.navigation_drawer);
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
}
