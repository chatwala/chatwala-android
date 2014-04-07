package com.chatwala.android.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import com.chatwala.android.R;
import com.chatwala.android.contacts.ContactEntry;
import com.chatwala.android.contacts.ContactsAdapter;
import com.chatwala.android.contacts.ContactsLoader;
import com.chatwala.android.contacts.FrequentContactsAdapter;
import com.chatwala.android.contacts.FrequentContactsLoader;
import com.chatwala.android.sms.Sms;
import com.chatwala.android.sms.SmsManager;
import com.chatwala.android.util.CWAnalytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsActivity extends FragmentActivity implements ContactsAdapter.OnContactActionListener {
    public static final String SMS_MESSAGE_URL_EXTRA = "sms_message_url";
    public static final String SMS_MESSAGE_EXTRA = "sms_message";

    private static final int MOST_CONTACTED_CONTACT_LIMIT = 27;

    private static final int CONTACTS_LOADER_CODE = 0;
    private static final int CONTACTS_TIME_CONTACTED_LOADER_CODE = 1;

    private final String analyticsCategory = CWAnalytics.getCategory();

    private String smsMessageUrl;
    private String smsMessage = null;

    private boolean sendAnalyticsBackgroundEvent = true;

    private Map<String, Boolean> contactsSentTo;

    private EditText contactsFilter;
    private ListView contactsListView;
    private GridView recentsGridView;

    private ContactsAdapter contactsAdapter;
    private FrequentContactsAdapter recentsdAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_sms);

        if(getIntent().hasExtra(SMS_MESSAGE_URL_EXTRA)) {
            smsMessageUrl = getIntent().getStringExtra(SMS_MESSAGE_URL_EXTRA);
        }
        else {
            finish();
            return;
        }
        if(getIntent().hasExtra(SMS_MESSAGE_EXTRA)) {
            smsMessage = getIntent().getStringExtra(SMS_MESSAGE_EXTRA);
        }

        //Typeface fontDemi = ((ChatwalaApplication) getApplication()).fontMd;
        //((TextView)findViewById(R.id.sms_copy)).setTypeface(fontDemi);

        contactsSentTo = new HashMap<String, Boolean>();

        contactsAdapter = new ContactsAdapter(this, new ArrayList<ContactEntry>(), true, this);
        recentsdAdapter = new FrequentContactsAdapter(this, new ArrayList<ContactEntry>(), false, this);

        contactsFilter = (EditText) findViewById(R.id.contacts_filter);
        contactsFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                //TODO put in postDelayed if not working
                if(s.toString().isEmpty()) {
                    contactsListView.setVisibility(View.GONE);
                    recentsGridView.setVisibility(View.VISIBLE);
                    findViewById(R.id.recent_contacts_lbl).setVisibility(View.VISIBLE);
                    findViewById(R.id.contacts_filter_clear).setVisibility(View.GONE);
                }
                else {
                    contactsListView.setVisibility(View.VISIBLE);
                    recentsGridView.setVisibility(View.GONE);
                    findViewById(R.id.recent_contacts_lbl).setVisibility(View.GONE);
                    findViewById(R.id.contacts_filter_clear).setVisibility(View.VISIBLE);
                }
                contactsAdapter.getFilter().filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
        });

        contactsListView = (ListView) findViewById(R.id.contacts_list);
        recentsGridView = (GridView) findViewById(R.id.recents_list);

        findViewById(R.id.contacts_filter_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(contactsFilter.getWindowToken(), 0);
                contactsFilter.setText("");
            }
        });

        getSupportLoaderManager().initLoader(CONTACTS_LOADER_CODE, null, contactsCallbacks);
        getSupportLoaderManager().initLoader(CONTACTS_TIME_CONTACTED_LOADER_CODE, null, frequentlyContactsCallbacks);
    }

    @Override
    public void onItemCheckedChange(ContactEntry contact, boolean isChecked) {
        if(!contact.isContact()) {
            onSend(contact);
        }
        else {
            if(isChecked) {
                onStartSend(contact);
            }
            else {
                contact.cancelSend();
                contactsSentTo.remove(contact.getName() + contact.getValue());
                notifyAdaptersDataSetChanged();
                CWAnalytics.sendMessageSendCanceledEvent();
            }
        }
    }

    @Override
    public void onStartSend(ContactEntry contact) {
        if(!contact.isSending()) {
            if(contactsSentTo.containsKey(contact.getName() + contact.getValue())) {
                contact.setIsSent(true);
                notifyAdaptersDataSetChanged();
            }
            else {
                contact.startSend(new ContactEntry.OnSendStateChangedListener() {
                    @Override
                    public void onSendStateChanged(ContactEntry contact, boolean isSent) {
                        if(isSent) {
                            onSend(contact);
                        }
                        notifyAdaptersDataSetChanged();
                    }
                });
                contactsSentTo.put(contact.getName() + contact.getValue(), true);
                if(contactsListView.isShown()) {
                    CWAnalytics.sendRecipientAddedEvent();
                }
                else if(recentsGridView.isShown()) {
                    CWAnalytics.sendRecentAddedEvent();
                }
            }
        }
    }

    @Override
    public void onSendCanceled(ContactEntry contact) {
        if(recentsGridView.isShown()) {
            contact.cancelSend();
            contactsSentTo.remove(contact.getName() + contact.getValue());
            notifyAdaptersDataSetChanged();
            CWAnalytics.sendMessageSendCanceledEvent();
        }
    }

    @Override
    public void onSend(ContactEntry contact) {
        if(!contact.isContact()) {
            CWAnalytics.sendNumberAddedEvent();
            contactsFilter.setText("");
        }
        SmsManager.getInstance().sendSms(new Sms(contact.getValue(), smsMessage, smsMessageUrl, analyticsCategory));
    }

    @Override
    public void onPause() {
        super.onPause();

        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(contactsFilter.getWindowToken(), 0);
    }

    @Override
    public void onStop() {
        super.onStop();

        finish();

        if(sendAnalyticsBackgroundEvent) {
            CWAnalytics.sendBackgroundWhileSmsEvent();
        }

    }

    private LoaderManager.LoaderCallbacks<List<ContactEntry>> contactsCallbacks = new LoaderManager.LoaderCallbacks<List<ContactEntry>>() {
        @Override
        public Loader onCreateLoader(int i, Bundle bundle) {
            return new ContactsLoader(SmsActivity.this);
        }

        @Override
        public void onLoadFinished(Loader<List<ContactEntry>> contactEntryLoader, List<ContactEntry> contacts) {
            contactsAdapter = new ContactsAdapter(SmsActivity.this, contacts, true, SmsActivity.this);
            contactsListView.setAdapter(contactsAdapter);
        }

        @Override
        public void onLoaderReset(Loader loader) {
            //do nothing
        }
    };

    private LoaderManager.LoaderCallbacks<List<ContactEntry>> frequentlyContactsCallbacks = new LoaderManager.LoaderCallbacks<List<ContactEntry>>() {
        @Override
        public Loader onCreateLoader(int i, Bundle bundle) {
            return new FrequentContactsLoader(SmsActivity.this, MOST_CONTACTED_CONTACT_LIMIT);
        }

        @Override
        public void onLoadFinished(Loader<List<ContactEntry>> listLoader, List<ContactEntry> contacts) {
            recentsdAdapter = new FrequentContactsAdapter(SmsActivity.this, contacts, true, SmsActivity.this);
            recentsGridView.setAdapter(recentsdAdapter);
        }

        @Override
        public void onLoaderReset(Loader loader) {
            //do nothing
        }
    };

    private void notifyAdaptersDataSetChanged() {
        contactsAdapter.notifyDataSetChanged();
        recentsdAdapter.notifyDataSetChanged();
    }
}
