package com.chatwala.android.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.Window;
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
    private static final int TOP_CONTACTS_LOADER_CODE = 1000;
    private static final int INITIAL_TOP_CONTACTS = 8;

    private TopContactsAdapter adapter;
    private GridView topContactsGrid;
    private TextView topContactsCountText;
    private String countTextTemplate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_top_contacts);

        getSupportLoaderManager().initLoader(TOP_CONTACTS_LOADER_CODE, null, this);

        topContactsGrid = (GridView) findViewById(R.id.top_contacts_grid);
        adapter = new TopContactsAdapter(this, new ArrayList<ContactEntry>(0), false, null);
        topContactsGrid.setAdapter(adapter);

        countTextTemplate = getString(R.string.top_contacts_bottom_text);
        topContactsCountText = (TextView) findViewById(R.id.top_contacts_count);
        Typeface fontDemi = ((ChatwalaApplication) getApplication()).fontMd;
        topContactsCountText.setTypeface(fontDemi);
        ((TextView) findViewById(R.id.top_contacts_header1)).setTypeface(fontDemi);
        ((TextView) findViewById(R.id.top_contacts_header2)).setTypeface(fontDemi);
        topContactsCountText.setText(String.format(countTextTemplate, INITIAL_TOP_CONTACTS));
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
        }

        CWAnalytics.sendTopContactsLoadedEvent(contacts.size());

        if(contacts.size() == 0) {
            startNewCameraActivity();
        }

        adapter = new TopContactsAdapter(TopContactsActivity.this, contacts, false, new TopContactsAdapter.TopContactsEventListener() {
            @Override
            public void onContactRemoved(int contactsLeft) {
                if(contactsLeft == 0) {
                    startNewCameraActivity();
                    return;
                }
                else if(contactsLeft == 1) {
                    topContactsCountText.setText(getString(R.string.top_contacts_bottom_text_singular));
                }
                else {
                    topContactsCountText.setText(String.format(countTextTemplate, contactsLeft));
                }
            }

            @Override
            public void onContactClicked() {
                //not currently used
            }

            @Override
            public void onSend() {
                CWAnalytics.sendTapNextEvent(true, adapter.getList().size());
                startNewCameraActivity();
            }
        });
        topContactsGrid.setAdapter(adapter);

        findViewById(R.id.top_contacts_top_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CWAnalytics.sendTapNextEvent(false, adapter.getList().size());
                startNewCameraActivity();
            }
        });

        findViewById(R.id.top_contacts_bottom_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CWAnalytics.sendTapNextEvent(false, adapter.getList().size());
                startNewCameraActivity();
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
        startActivity(i);
        finish();
    }
}
