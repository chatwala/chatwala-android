package com.chatwala.android.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.TextView;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.R;
import com.chatwala.android.contacts.ContactEntry;
import com.chatwala.android.contacts.FrequentContactsLoader;
import com.chatwala.android.contacts.TopContactsAdapter;
import com.chatwala.android.util.CWAnalytics;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eliezer on 4/1/2014.
 */
public class TopContactsActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<List<ContactEntry>> {
    public static final String TOP_CONTACTS_LIST_EXTRA = "TOP_CONTACTS_LIST";
    public static final String TOP_CONTACTS_SHOW_UPSELL_EXTRA = "TOP_CONTACTS_SHOW_UPSELL";
    private static final int TOP_CONTACTS_LOADER_CODE = 1000;
    public static final int INITIAL_TOP_CONTACTS = 9;

    private TopContactsAdapter adapter;
    private GridView topContactsGrid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_top_contacts);

        getSupportLoaderManager().initLoader(TOP_CONTACTS_LOADER_CODE, null, this);

        topContactsGrid = (GridView) findViewById(R.id.top_contacts_grid);
        adapter = new TopContactsAdapter(this, new ArrayList<ContactEntry>(0), false, null);
        topContactsGrid.setAdapter(adapter);

        Typeface fontDemi = ((ChatwalaApplication) getApplication()).fontMd;
        ((TextView) findViewById(R.id.top_contacts_header)).setTypeface(fontDemi);
       // findViewById(R.id.top_contacts_bottom).setVisibility(View.INVISIBLE);
        findViewById(R.id.top_contacts_start_button).setVisibility(View.GONE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_contacts_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_top_contacts_skip) {
            adapter.clearContactsToSendTo();
            startNewCameraActivity();
            CWAnalytics.sendTapSkipEvent();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<List<ContactEntry>> onCreateLoader(int i, Bundle bundle) {
        return new FrequentContactsLoader(TopContactsActivity.this, INITIAL_TOP_CONTACTS);
    }

    @Override
    public void onLoadFinished(Loader<List<ContactEntry>> contactEntryLoader, List<ContactEntry> contacts) {
        if(contacts == null) {
            CWAnalytics.sendTopContactsLoadedEvent(0);
            startNewCameraActivity();
            return;
        }

        CWAnalytics.sendTopContactsLoadedEvent(contacts.size());

        if(contacts.size() == 0) {
            startNewCameraActivity();
        }

        adapter = new TopContactsAdapter(TopContactsActivity.this, contacts, false, new TopContactsAdapter.TopContactsEventListener() {
            @Override
            public void onContactRemoved(int numContacts) {
               /* if(contactsLeft == 0) {
                    startNewCameraActivity();
                    return;
                }*/
                if(numContacts==0) {
                    findViewById(R.id.top_contacts_start_button).setVisibility(View.GONE);
                    findViewById(R.id.top_contacts_bottom_text).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onContactClicked(int numContacts) {
                //not currently used
                if(numContacts>0) {
                    //show send button
                    findViewById(R.id.top_contacts_start_button).setVisibility(View.VISIBLE);
                    findViewById(R.id.top_contacts_bottom_text).setVisibility(View.GONE);
                }

            }

            @Override
            public void onSend() {
                CWAnalytics.sendTapNextEvent(true, adapter.getContactsToSendTo().size());
                startNewCameraActivity();
            }
        });
        topContactsGrid.setAdapter(adapter);


        findViewById(R.id.top_contacts_top).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(adapter.getContactsToSendTo().size()>0) {
                    CWAnalytics.sendTapNextEvent(false, adapter.getContactsToSendTo().size());
                    startNewCameraActivity();
                }
            }
        });

        findViewById(R.id.top_contacts_start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(adapter.getContactsToSendTo().size()>0) {
                    CWAnalytics.sendTapNextEvent(true, adapter.getContactsToSendTo().size());
                    startNewCameraActivity();
                }
            }
        });

        findViewById(R.id.top_contacts_bottom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(adapter.getContactsToSendTo().size()>0) {
                    CWAnalytics.sendTapNextEvent(false, adapter.getContactsToSendTo().size());
                    startNewCameraActivity();
                }
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<List<ContactEntry>> contactEntryLoader) {
        //do nothing
    }

    private void startNewCameraActivity() {
        Intent i = new Intent(TopContactsActivity.this, NewCameraActivity.class);
        i.putStringArrayListExtra(TOP_CONTACTS_LIST_EXTRA, adapter.getContactsToSendTo());
        if(adapter.getCount() == INITIAL_TOP_CONTACTS) {
            i.putExtra(TOP_CONTACTS_SHOW_UPSELL_EXTRA, true);
        }
        startActivity(i);
        finish();
    }
}
