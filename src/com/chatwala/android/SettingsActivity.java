package com.chatwala.android;

import android.content.Context;
import android.content.Intent;
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
    Spinner deliveryMethodSpinner;

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

    enum DeliveryOptions
    {
        SMS,
        Email
    }
}
