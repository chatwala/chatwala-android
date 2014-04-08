package com.chatwala.android.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import com.chatwala.android.R;
import com.chatwala.android.SmsSentReceiver;
import com.chatwala.android.contacts.ContactEntry;
import com.chatwala.android.contacts.ContactsAdapter;
import com.chatwala.android.contacts.ContactsLoader;
import com.chatwala.android.contacts.FrequentContactsAdapter;
import com.chatwala.android.contacts.FrequentContactsLoader;
import com.chatwala.android.util.CWAnalytics;
import com.chatwala.android.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmsActivity extends FragmentActivity {
    public static final String SMS_MESSAGE_URL_EXTRA = "sms_message_url";
    public static final String SMS_MESSAGE_EXTRA = "sms_message";

    private static final int MAX_SMS_MESSAGE_LENGTH = 160;

    private static final int MOST_CONTACTED_CONTACT_LIMIT = 27;

    private static final int CONTACTS_LOADER_CODE = 0;
    private static final int CONTACTS_TIME_CONTACTED_LOADER_CODE = 1;

    private String smsMessageUrl;
    private String smsMessage;

    private boolean sendAnalyticsBackgroundEvent = true;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private int messagesSent = 0;

    private Map<String, Boolean> contactsSentTo;

    private EditText contactsFilter;
    private ListView contactsListView;
    private GridView recentsGridView;

    private ContactsAdapter contactsAdapter;
    private FrequentContactsAdapter recentsdAdapter;

    private class SendMessageRunnable implements Runnable {
        private Context appContext;
        private String value;

        public SendMessageRunnable(String value) {
            this.value = value;
            if(SmsActivity.this != null) {
                appContext = SmsActivity.this.getApplicationContext();
            }
        }

        @Override
        public void run() {
            //this isn't needed unless we allow for custom messages
            /*String messageUrlWithPrefix = ": " + smsMessageUrl;
            int maxMessageSize = MAX_SMS_MESSAGE_LENGTH - messageUrlWithPrefix.length();
            if(smsMessage.length() > maxMessageSize) {
                smsMessage = smsMessage.substring(maxMessageSize - 1) + messageUrlWithPrefix;
            }*/
            String message = smsMessage + ": " + smsMessageUrl;
            PendingIntent sentIntent = null;
            if(appContext != null) {
                sentIntent = PendingIntent.getBroadcast(appContext, messagesSent, new Intent(appContext, SmsSentReceiver.class), 0);
            }
            try {
                SmsManager.getDefault().sendTextMessage(value, null, message, sentIntent, null);
                messagesSent++;
            }
            catch(Exception e) {
                Logger.e("There was an exception while sending SMS(s)", e);
                CWAnalytics.sendMessageSentFailedEvent();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_sms);

        if(!getIntent().hasExtra(SMS_MESSAGE_URL_EXTRA) || !getIntent().hasExtra(SMS_MESSAGE_EXTRA)) {
            finish();
            return;
        }

        //Typeface fontDemi = ((ChatwalaApplication) getApplication()).fontMd;
        //((TextView)findViewById(R.id.sms_copy)).setTypeface(fontDemi);

        smsMessageUrl = getIntent().getStringExtra(SMS_MESSAGE_URL_EXTRA);
        smsMessage = getIntent().getStringExtra(SMS_MESSAGE_EXTRA);

        contactsSentTo = new HashMap<String, Boolean>();

        contactsAdapter = new ContactsAdapter(this, new ArrayList<ContactEntry>(), true);
        setContactsAdapterItemCheckedChangeListener();
        recentsdAdapter = new FrequentContactsAdapter(this, new ArrayList<ContactEntry>(), false);
        setFrequentsAdapterItemCheckedChangeListener();

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
        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final ContactEntry entry = contactsAdapter.getItem(i);
                entry.setOnSendListener(new ContactEntry.OnSendListener() {
                    @Override
                    public void onSend() {
                        sendSms(entry);
                    }
                });
                contactsAdapter.setItemOnSendStateChangedListener(entry);
                if(!entry.isContact()) {
                    sendSms(entry);
                    contactsFilter.setText("");
                    return;
                }

                if(!entry.isSending()) {
                    if(contactsSentTo.containsKey(entry.getName() + entry.getValue())) {
                        entry.setIsSent(true);
                        contactsAdapter.notifyDataSetChanged();
                        recentsdAdapter.notifyDataSetChanged();
                    }
                    else {
                        contactsSentTo.put(entry.getName() + entry.getValue(), true);
                        startSendSms(entry);
                    }
                }
            }
        });

        recentsGridView = (GridView) findViewById(R.id.recents_list);
        recentsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final ContactEntry entry = recentsdAdapter.getItem(i);
                recentsdAdapter.setItemOnSendStateChangedListener(entry);
                entry.setOnSendListener(new ContactEntry.OnSendListener() {
                    @Override
                    public void onSend() {
                        sendSms(entry);
                    }
                });

                if(!entry.isContact()) {
                    sendSms(entry);
                    contactsFilter.setText("");
                    return;
                }

                if(entry.isSending()) {
                    contactsSentTo.remove(entry.getName() + entry.getValue());
                    onCancelSendSms(entry);
                }
                else {
                    if(contactsSentTo.containsKey(entry.getName() + entry.getValue())) {
                        entry.setIsSent(true);
                        contactsAdapter.notifyDataSetChanged();
                        recentsdAdapter.notifyDataSetChanged();
                    }
                    else {
                        contactsSentTo.put(entry.getName() + entry.getValue(), true);
                        startSendSms(entry);
                    }
                }
            }
        });

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

    private void setContactsAdapterItemCheckedChangeListener() {
        contactsAdapter.setOnItemCheckedChangeListener(new ContactsAdapter.OnItemCheckedChangeListener() {
            @Override
            public void onItemCheckedChanged(final ContactEntry entry, boolean isChecked) {
                contactsAdapter.setItemOnSendStateChangedListener(entry);
                entry.setOnSendListener(new ContactEntry.OnSendListener() {
                    @Override
                    public void onSend() {
                        sendSms(entry);
                    }
                });

                if(!entry.isContact()) {
                    entry.sendMessage();
                    contactsFilter.setText("");
                    return;
                }

                if(isChecked) {
                    if(contactsSentTo.containsKey(entry.getName() + entry.getValue())) {
                        entry.setIsSent(true);
                    }
                    else {
                        contactsSentTo.put(entry.getName() + entry.getValue(), true);
                        entry.startSend();
                    }
                }
                else {
                    contactsSentTo.remove(entry.getName() + entry.getValue());
                    entry.cancelSend();
                }

                contactsAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setFrequentsAdapterItemCheckedChangeListener() {
        recentsdAdapter.setOnItemCheckedChangeListener(new ContactsAdapter.OnItemCheckedChangeListener() {
            @Override
            public void onItemCheckedChanged(final ContactEntry entry, boolean isChecked) {
                recentsdAdapter.setItemOnSendStateChangedListener(entry);
                entry.setOnSendListener(new ContactEntry.OnSendListener() {
                    @Override
                    public void onSend() {
                        sendSms(entry);
                    }
                });

                if(isChecked) {
                    if(contactsSentTo.containsKey(entry.getName() + entry.getValue())) {
                        entry.setIsSent(true);
                    }
                    else {
                        contactsSentTo.put(entry.getName() + entry.getValue(), true);
                        entry.startSend();
                    }
                }
                else {
                    contactsSentTo.remove(entry.getName() + entry.getValue());
                    entry.cancelSend();
                }

                recentsdAdapter.notifyDataSetChanged();
            }
        });
    }

    private void startSendSms(ContactEntry contact) {
        contact.startSend();
        if(contactsListView.isShown()) {
            CWAnalytics.sendRecipientAddedEvent();
        }
        else if(recentsGridView.isShown()) {
            CWAnalytics.sendRecentAddedEvent();
        }
    }

    private void onCancelSendSms(ContactEntry contact) {
        contact.cancelSend();
        contactsAdapter.notifyDataSetChanged();
        recentsdAdapter.notifyDataSetChanged();
        CWAnalytics.sendMessageSendCanceledEvent();
    }

    private void sendSms(ContactEntry contact) {
        executor.execute(new SendMessageRunnable(contact.getValue()));
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

        if(messagesSent > 0) {
            CWAnalytics.sendMessageSentEvent(messagesSent);
        }

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
            contactsAdapter = new ContactsAdapter(SmsActivity.this, contacts, true);
            setContactsAdapterItemCheckedChangeListener();
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
            recentsdAdapter = new FrequentContactsAdapter(SmsActivity.this, contacts, true);
            setFrequentsAdapterItemCheckedChangeListener();
            recentsGridView.setAdapter(recentsdAdapter);
        }

        @Override
        public void onLoaderReset(Loader loader) {
            //do nothing
        }
    };
}
