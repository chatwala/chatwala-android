package com.chatwala.android.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.chatwala.android.*;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.MessageDataStore;
import xmlwise.Plist;
import xmlwise.XmlParseException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/27/13
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class SettingsActivity extends BaseChatWalaActivity
{
    Spinner messagePreviewSpinner, deliveryMethodSpinner, refreshIntervalSpinner, diskSpaceSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        messagePreviewSpinner = (Spinner)findViewById(R.id.message_preview_spinner);
        messagePreviewSpinner.setAdapter(new PreviewMessageAdapter());
        messagePreviewSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //catching crash: https://www.crashlytics.com/chatwala2/android/apps/com.chatwala.chatwala/issues/53050910fabb27481bfb58e8
                if(view==null) {
                    return;
                }

                PreviewMessageOptions selected = (PreviewMessageOptions) view.getTag();
                switch (selected) {
                    case ON:
                        AppPrefs.getInstance(SettingsActivity.this).setPrefShowPreview(true);
                        //TODO previewMessge Analytics? CWLog.logUsingSms(true);
                        break;
                    case OFF:
                        AppPrefs.getInstance(SettingsActivity.this).setPrefShowPreview(false);
                        //TODO previewMessge Analytics? CWLog.logUsingSms(false);
                        break;
                    default:
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        messagePreviewSpinner.setSelection(AppPrefs.getInstance(SettingsActivity.this).getPrefShowPreview() ? 0 : 1);

        deliveryMethodSpinner = (Spinner)findViewById(R.id.delivery_spinner);
        deliveryMethodSpinner.setAdapter(new DeliveryMethodAdapter());
        deliveryMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                //catching crash: https://www.crashlytics.com/chatwala2/android/apps/com.chatwala.chatwala/issues/53050910fabb27481bfb58e8
                if(view==null) {
                    return;
                }

                DeliveryMethod selected = (DeliveryMethod) view.getTag();
                AppPrefs.getInstance(SettingsActivity.this).setDeliveryMethod(selected);
                Logger.logDeliveryMethod(selected.getDisplayString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
        deliveryMethodSpinner.setSelection(AppPrefs.getInstance(SettingsActivity.this).getDeliveryMethod().getMethod());

        refreshIntervalSpinner = (Spinner)findViewById(R.id.refresh_spinner);
        refreshIntervalSpinner.setAdapter(new RefreshIntervalAdapter());
        refreshIntervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                RefreshOptions selected = (RefreshOptions)view.getTag();
                AppPrefs.getInstance(SettingsActivity.this).setPrefMessageLoadInterval(selected.getInterval());
                Logger.logRefreshInterval((selected.getInterval()));
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
                Logger.logStorageLimit(selected.getSpace());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
        diskSpaceSpinner.setSelection(DiskSpaceOptions.fromSpace(AppPrefs.getInstance(SettingsActivity.this).getPrefDiskSpaceMax()).getSortOrder());

        findViewById(R.id.edit_profile_pic_row).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                UpdateProfilePicActivity.startMe(SettingsActivity.this, false);
            }
        });

        findViewById(R.id.terms_and_conditions_row).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(SettingsActivity.this, ChatwalaWebActivity.class);
                intent.putExtra(ChatwalaWebActivity.CHATWALA_WEB_TITLE_EXTRA, getString(R.string.terms_and_conditions));
                intent.putExtra(ChatwalaWebActivity.CHATWALA_WEB_URL_EXTRA, "http://www.chatwala.com/tos");
                startActivity(intent);
            }
        });

        findViewById(R.id.privacy_policy_row).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(SettingsActivity.this, ChatwalaWebActivity.class);
                intent.putExtra(ChatwalaWebActivity.CHATWALA_WEB_TITLE_EXTRA, getString(R.string.privacy_policy));
                intent.putExtra(ChatwalaWebActivity.CHATWALA_WEB_URL_EXTRA, "http://www.chatwala.com/privacy");
                startActivity(intent);
            }
        });

        findViewById(R.id.feedback_row).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                FeedbackActivity.startMe(SettingsActivity.this, false);
            }
        });

        findViewById(R.id.version_info_row).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String appVersion;
                try {
                    appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    appVersion = "unknown";
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(R.string.version_information);
                StringBuilder message = new StringBuilder("Version: ").append(appVersion).append("\nServer: ").append(EnvironmentVariables.get().getDisplayString());
                builder.setMessage(message);
                builder.show();
            }
        });

        if(EnvironmentVariables.get().getCanSwitchUser()) {
            final AppPrefs prefs = AppPrefs.getInstance(this);
            String overridingUserId = null;
            boolean isUserIdOverridden = prefs.isUserIdOverridden();

            findViewById(R.id.dev_features_container).setVisibility(View.VISIBLE);
            CheckBox overrideUserCb = (CheckBox) findViewById(R.id.override_user_cb);
            final EditText overrideUserIdEt = (EditText) findViewById((R.id.override_user_box));

            overrideUserCb.setChecked(isUserIdOverridden);
            overrideUserCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    if(isChecked) {
                        String newId = overrideUserIdEt.getText().toString().trim();
                        if(!newId.isEmpty()) {
                            prefs.overrideUserId(overrideUserIdEt.getText().toString());
                        }
                    }
                    else {
                        prefs.restoreUserId();
                    }
                }
            });

            if(isUserIdOverridden) {
                overridingUserId = prefs.getUserId();
                overrideUserIdEt.setText(overridingUserId);
            }

        }
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

    class PreviewMessageAdapter extends BaseAdapter {
        @Override
        public int getCount()
        {
            return PreviewMessageOptions.values().length;
        }

        @Override
        public Object getItem(int position) {
            return PreviewMessageOptions.values()[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
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

    class DeliveryMethodAdapter extends BaseAdapter
    {
        private DeliveryMethod[] methods;

        public DeliveryMethodAdapter() {
            methods = DeliveryMethod.values();
            try {
                if(!ChatwalaApplication.isChatwalaSmsEnabled()) {
                    DeliveryMethod[] newMethods = new DeliveryMethod[methods.length - 1];
                    for(int i = 0; i < methods.length; i++) {
                        if(methods[i] == DeliveryMethod.CWSMS) {
                            while(++i < methods.length) {
                                newMethods[i - 1] = methods[i];
                            }
                            break;
                        }
                        else {
                            newMethods[i] = methods[i];
                        }
                    }
                    methods = newMethods;
                }
            }
            catch (Exception e) {
                Logger.e("Exception with disabling ChatwalaSMS", e);
            }
        }

        @Override
        public int getCount()
        {
            return methods.length;
        }

        @Override
        public DeliveryMethod getItem(int position)
        {
            return methods[position];
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

    enum PreviewMessageOptions {
        ON,
        OFF
    }

    public static enum DeliveryMethod {
        SMS(0, "SMS"),
        CWSMS(1, "Chatwala SMS"),
        EMAIL(2, "Email"),
        FB(3, "Facebook");

        private int method;
        private String displayString;

        DeliveryMethod(int method, String displayString) {
            this.method = method;
            this.displayString = displayString;
        }

        public int getMethod() {
            return method;
        }

        public String getDisplayString() {
            return displayString;
        }
    }

    enum RefreshOptions
    {
        ONE_MINUTE(1, "One Minute", 0),
        FIVE_MINUTES(5, "Five Minutes", 1),
        TEN_MINUTES(10, "Ten Minutes", 2),
        THIRTY_MINUTES(30, "Thirty Minutes", 3),
        ONE_HOUR(60, "One Hour", 4),
        TWO_HOURS(120, "Two Hours", 5);

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
            return ONE_HOUR;
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
