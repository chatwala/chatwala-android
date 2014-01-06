package com.chatwala.android;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/27/13
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class SettingsActivity extends BaseChatWalaActivity
{
    Spinner deliveryMethodSpinner, refreshIntervalSpinner, diskSpaceSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        deliveryMethodSpinner = (Spinner)findViewById(R.id.delivery_spinner);
        deliveryMethodSpinner.setAdapter(new DeliveryMethodAdapter());
        deliveryMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                DeliveryOptions selected = (DeliveryOptions) view.getTag();
                switch (selected)
                {
                    case SMS:
                        AppPrefs.getInstance(SettingsActivity.this).setPrefUseSms(true);
                        break;
                    case Email:
                        AppPrefs.getInstance(SettingsActivity.this).setPrefUseSms(false);
                        break;
                    default:
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
        deliveryMethodSpinner.setSelection(AppPrefs.getInstance(SettingsActivity.this).getPrefUseSms() ? 0 : 1);

        refreshIntervalSpinner = (Spinner)findViewById(R.id.refresh_spinner);
        refreshIntervalSpinner.setAdapter(new RefreshIntervalAdapter());
        refreshIntervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                RefreshOptions selected = (RefreshOptions)view.getTag();
                AppPrefs.getInstance(SettingsActivity.this).setPrefMessageLoadInterval(selected.getInterval());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
        refreshIntervalSpinner.setSelection(RefreshOptions.fromInterval(AppPrefs.getInstance(SettingsActivity.this).getPrefMessageLoadInterval()).getSortOrder());

        diskSpaceSpinner = (Spinner)findViewById(R.id.disk_space_spinner);
        diskSpaceSpinner.setAdapter(new DiskSpaceAdapter());
        diskSpaceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                DiskSpaceOptions selected = (DiskSpaceOptions)view.getTag();
                AppPrefs.getInstance(SettingsActivity.this).setPrefDiskSpaceMax(selected.getSpace());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
        diskSpaceSpinner.setSelection(DiskSpaceOptions.fromSpace(AppPrefs.getInstance(SettingsActivity.this).getPrefDiskSpaceMax()).getSortOrder());


        findViewById(R.id.terms_and_conditions_row).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Uri uri = Uri.parse("http://www.chatwala.com/tos");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        findViewById(R.id.privacy_policy_row).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Uri uri = Uri.parse("http://www.chatwala.com/privacy");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        String appVersion;
        try
        {
            appVersion = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            appVersion = "unknown";
        }

        ((TextView)findViewById(R.id.version_info_text)).setText("Version: " + appVersion);
        ((TextView)findViewById(R.id.api_info_text)).setText("Server: " + ChatwalaApplication.getApiPathString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static void startMe(Context context)
    {
        context.startActivity(new Intent(context, SettingsActivity.class));
    }

    class DeliveryMethodAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return DeliveryOptions.values().length;
        }

        @Override
        public Object getItem(int position)
        {
            return DeliveryOptions.values()[position];
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
                convertView = new TextView(SettingsActivity.this);
                int viewHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
                ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, viewHeight);
                convertView.setLayoutParams(params);
                ((TextView)convertView).setGravity(Gravity.CENTER_VERTICAL);
                ((TextView)convertView).setTextColor(getResources().getColor(R.color.text_white));
                convertView.setBackgroundColor(getResources().getColor(R.color.settings_button_background));
            }

            convertView.setTag(getItem(position));
            ((TextView)convertView).setText(getItem(position).toString());

            return convertView;
        }
    }

    class RefreshIntervalAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return RefreshOptions.values().length;
        }

        @Override
        public RefreshOptions getItem(int position)
        {
            return RefreshOptions.values()[position];
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
                convertView = new TextView(SettingsActivity.this);
                int viewHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
                ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, viewHeight);
                convertView.setLayoutParams(params);
                ((TextView)convertView).setGravity(Gravity.CENTER_VERTICAL);
                ((TextView)convertView).setTextColor(getResources().getColor(R.color.text_white));
                convertView.setBackgroundColor(getResources().getColor(R.color.settings_button_background));
            }

            convertView.setTag(getItem(position));
            ((TextView)convertView).setText(getItem(position).getDisplayString());

            return convertView;
        }
    }

    class DiskSpaceAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return DiskSpaceOptions.values().length;
        }

        @Override
        public DiskSpaceOptions getItem(int position)
        {
            return DiskSpaceOptions.values()[position];
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
                convertView = new TextView(SettingsActivity.this);
                int viewHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
                ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, viewHeight);
                convertView.setLayoutParams(params);
                ((TextView)convertView).setGravity(Gravity.CENTER_VERTICAL);
                ((TextView)convertView).setTextColor(getResources().getColor(R.color.text_white));
                convertView.setBackgroundColor(getResources().getColor(R.color.settings_button_background));
            }

            convertView.setTag(getItem(position));
            ((TextView)convertView).setText(getItem(position).getDisplayString());

            return convertView;
        }
    }

    enum DeliveryOptions
    {
        SMS,
        Email
    }

    enum RefreshOptions
    {
        FIVE_MINUTES(5, "Five Minutes", 0),
        TEN_MINUTES(10, "Ten Minutes", 1),
        THIRTY_MINUTES(30, "Thirty Minutes", 2),
        ONE_HOUR(60, "One Hour", 3),
        TWO_HOURS(120, "Two Hours", 4);

        private int interval, sortOrder;
        private String displayString;

        private RefreshOptions(int interval, String displayString, int sortOrder)
        {
            this.interval = interval;
            this.displayString = displayString;
            this.sortOrder = sortOrder;
        }

        public int getInterval()
        {
            return interval;
        }

        public String getDisplayString()
        {
            return displayString;
        }

        public int getSortOrder()
        {
            return sortOrder;
        }

        public static RefreshOptions fromInterval(int interval)
        {
            for (RefreshOptions item : RefreshOptions.values())
            {
                if (item.getInterval() == interval)
                {
                    return item;
                }
            }
            //If something goes wrong, return the default
            return TWO_HOURS;
        }
    }

    enum DiskSpaceOptions
    {
        TEN_MEGS(10, "10 MB", 0),
        FIFTY_MEGS(50, "50 MB", 1),
        ONE_HUNDRED_MEGS(100, "100 MB", 2),
        FIVE_HUNDRED_MEGS(500, "500 MB", 3);

        private int space, sortOrder;
        private String displayString;

        private DiskSpaceOptions(int space, String displayString, int sortOrder)
        {
            this.space = space;
            this.displayString = displayString;
            this.sortOrder = sortOrder;
        }

        public int getSpace()
        {
            return space;
        }

        public String getDisplayString()
        {
            return displayString;
        }

        public int getSortOrder()
        {
            return sortOrder;
        }

        public static DiskSpaceOptions fromSpace(int space)
        {
            for (DiskSpaceOptions item : DiskSpaceOptions.values())
            {
                if (item.getSpace() == space)
                {
                    return item;
                }
            }
            //If something goes wrong, return the default
            return FIVE_HUNDRED_MEGS;
        }
    }
}
