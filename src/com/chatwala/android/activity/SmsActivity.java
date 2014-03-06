package com.chatwala.android.activity;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.R;
import com.chatwala.android.SmsSentReceiver;
import com.chatwala.android.util.CWAnalytics;
import com.chatwala.android.util.Logger;

import javax.sql.CommonDataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SmsActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String SMS_MESSAGE_URL_EXTRA = "sms_message_url";
    public static final String SMS_MESSAGE_EXTRA = "sms_message";

    private static final int MAX_SMS_MESSAGE_LENGTH = 160;

    private static final int CONTACTS_LOADER_CODE = 0;

    private String smsMessageUrl;
    private String smsMessage;

    private boolean sendAnalyticsBackgroundEvent = true;

    private AutoCompleteTextView contactsFilter;
    private ListView recipientsListView;

    private ContactEntryAdapter contactsAdapter;
    private ContactEntryAdapter recipientsAdapter;

    private String[] contactsProjection = new String[] {ContactsContract.CommonDataKinds.Phone._ID,
                                                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                                                        ContactsContract.CommonDataKinds.Phone.TYPE};

    private Toast noContactsToast;

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

        Typeface fontDemi = ((ChatwalaApplication) getApplication()).fontMd;
        ((TextView)findViewById(R.id.sms_copy)).setTypeface(fontDemi);

        smsMessageUrl = getIntent().getStringExtra(SMS_MESSAGE_URL_EXTRA);
        smsMessage = getIntent().getStringExtra(SMS_MESSAGE_EXTRA);

        contactsAdapter = new ContactEntryAdapter(new ArrayList<ContactEntry>(), true, true);
        recipientsAdapter = new ContactEntryAdapter(new ArrayList<ContactEntry>(), false, false);

        contactsFilter = (AutoCompleteTextView) findViewById(R.id.contacts_filter);
        contactsFilter.setHintTextColor(Color.WHITE);
        contactsFilter.setThreshold(0);
        contactsFilter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CWAnalytics.sendRecipientAddedEvent();
                ContactEntry entry = contactsAdapter.getItem(i);
                contactsFilter.setText("");
                if (contactsAdapter.remove(entry)) {
                    recipientsAdapter.add(entry);
                }
            }
        });

        contactsFilter.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(contactsFilter, InputMethodManager.SHOW_FORCED);
            }
        }, 100);


        recipientsListView = (ListView) findViewById(R.id.recipients_list);
        recipientsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ContactEntry entry = recipientsAdapter.getItem(i);
                if (recipientsAdapter.remove(entry)) {
                    if (entry.isContact()) {
                        contactsAdapter.add(entry);
                    }
                }
            }
        });

        findViewById(R.id.send_sms_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(contactsFilter.getWindowToken(), 0);

                String enteredNumber = null;
                //if there are no recipients, check the filter to see if there's a number
                //if there is, send it, otherwise toast error
                if(recipientsAdapter.isEmpty()) {
                    String filterString = contactsFilter.getText().toString().trim();
                    if(filterString.isEmpty()) {
                        showErrorToast("Please enter a recipient");
                        return;
                    }

                    enteredNumber = PhoneNumberUtils.extractNetworkPortion(filterString);
                    if(enteredNumber.isEmpty()) {
                        showErrorToast("Please enter a valid recipient");
                        return;
                    }

                }
                new SendSmsAsyncTask(enteredNumber).execute(new Void[]{});
            }
        });

        getSupportLoaderManager().initLoader(CONTACTS_LOADER_CODE, null, this);
    }

    private void showErrorToast(String error) {
        if (noContactsToast != null) {
            noContactsToast.cancel();
        }
        noContactsToast = Toast.makeText(SmsActivity.this, error, Toast.LENGTH_SHORT);
        noContactsToast.show();
        return;
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

    @Override
    public Loader<Cursor> onCreateLoader(int loaderCode, Bundle bundle) {
        if(loaderCode == CONTACTS_LOADER_CODE) {
            return new CursorLoader(SmsActivity.this,
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    contactsProjection,
                                    null,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY);
        }
        else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<ContactEntry> contacts = new ArrayList<ContactEntry>(cursor.getCount());

        if(cursor.moveToFirst()) {
            do {
                try {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String value = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String type = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE));
                    type = getTypeString(type);
                    contacts.add(new ContactEntry(name, value, type, true));
                }
                catch(Exception e) {
                    continue;
                }
            } while(cursor.moveToNext());
        }

        contactsAdapter = new ContactEntryAdapter(contacts, true, true);
        contactsFilter.setAdapter(contactsAdapter);

        recipientsAdapter = new ContactEntryAdapter(new ArrayList<ContactEntry>(), false, false);
        recipientsListView.setAdapter(recipientsAdapter);
    }

    private String getTypeString(String typeStr) {
        int type = Integer.parseInt(typeStr);
        if(type == ContactsContract.CommonDataKinds.Phone.TYPE_HOME) {
            return "Home";
        }
        else if(type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
            return "Mobile";
        }
        else if(type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK) {
            return "Work";
        }
        else if(type == ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME) {
            return "Fax Home";
        }
        else if(type == ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK) {
            return "Fax Work";
        }
        else {
            return "Other";
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        //do nothing
    }

    private class SendSmsAsyncTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog pd;
        private String contactsFilterEnteredValue;

        public SendSmsAsyncTask(String contactsFilterEnteredValue) {
            this.contactsFilterEnteredValue = contactsFilterEnteredValue;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = ProgressDialog.show(SmsActivity.this, "Sending SMS", "Please Wait...");
        }

        @Override
        protected Void doInBackground(Void... v) {
            String messageUrlWithPrefix = "... " + smsMessageUrl;
            int maxMessageSize = MAX_SMS_MESSAGE_LENGTH - messageUrlWithPrefix.length();
            if(smsMessage.length() > maxMessageSize) {
                smsMessage = smsMessage.substring(maxMessageSize - 1) + messageUrlWithPrefix;
            }
            String message = smsMessage + "... " + smsMessageUrl;

            if(contactsFilterEnteredValue != null && !contactsFilterEnteredValue.isEmpty()) {
                PendingIntent sentIntent = PendingIntent.getBroadcast(SmsActivity.this, 0,
                        new Intent(SmsActivity.this, SmsSentReceiver.class), 0);

                doSendMessage(contactsFilterEnteredValue, message, sentIntent);
                CWAnalytics.sendMessageSentEvent(1);
            }
            else {
                for(int i = 0; i < recipientsAdapter.getCount(); i++) {
                    PendingIntent sentIntent = PendingIntent.getBroadcast(SmsActivity.this, i,
                            new Intent(SmsActivity.this, SmsSentReceiver.class), 0);

                    doSendMessage(recipientsAdapter.getItem(i).getValue(), message, sentIntent);
                }
                CWAnalytics.sendMessageSentEvent(recipientsAdapter.getCount());
            }

            return null;
        }

        private void doSendMessage(String recipient, String message, PendingIntent sentIntent) {
            try {
                SmsManager.getDefault().sendTextMessage(recipient, null, message, sentIntent, null);
            }
            catch(Exception e) {
                Logger.e("There was an exception while sending SMS(s)", e);
                CWAnalytics.sendMessageSentFailedEvent();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if(pd != null && pd.isShowing()) {
                pd.dismiss();
            }
            sendAnalyticsBackgroundEvent = false;
            finish();
        }
    }

    private class ContactEntry implements Comparable<ContactEntry> {
        private String name;
        private String value;
        private String type;
        private boolean isContact;

        public ContactEntry(String name, String value, String type, boolean isContact) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.isContact = isContact;
        }

        public String getName() {
            return (name == null ? "" : name);
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return (value == null ? "" : value);
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return (type == null ? "" : type);
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isContact() { return isContact; }

        public boolean equals(ContactEntry other) {
            return compareTo(other) == 0;
        }

        public int hashCode() {
            return (getName() + getValue()).hashCode();
        }

        @Override
        public int compareTo(ContactEntry other) {
            if(other == null) {
                return -1;
            }

            String name = getName();
            String otherName = other.getName();

            if(!name.isEmpty() && Character.isDigit(name.charAt(0))) {
                name = "_" + name;
            }

            if(!otherName.isEmpty() && Character.isDigit(otherName.charAt(0))) {
                otherName = "_" + otherName;
            }

            int compare = name.compareToIgnoreCase(otherName);
            if(compare == 0) {
                return getValue().compareToIgnoreCase(other.getValue());
            }
            else {
                return compare;
            }
        }
    }

    private class ContactEntryAdapter extends BaseAdapter implements Filterable {
        private List<ContactEntry> contacts;
        private List<ContactEntry> filteredContacts;
        private boolean useFiltered;
        private boolean showNumbers;
        private LayoutInflater inflater;

        public ContactEntryAdapter(List<ContactEntry> contacts, boolean useFiltered, boolean showNumbers) {
            this.contacts = contacts;
            Collections.sort(this.contacts);
            filteredContacts = new ArrayList<ContactEntry>(this.contacts);
            this.useFiltered = useFiltered;
            this.showNumbers = showNumbers;
            inflater = LayoutInflater.from(SmsActivity.this);
        }

        public List<ContactEntry> getList() {
            if(useFiltered) {
                return filteredContacts;
            }
            else {
                return contacts;
            }
        }

        @Override
        public int getCount() {
            return getList().size();
        }

        @Override
        public ContactEntry getItem(int i) {
            return getList().get(i);
        }

        public boolean remove(ContactEntry entry) {
            int index = Collections.binarySearch(contacts, entry);
            int filteredIndex = Collections.binarySearch(filteredContacts, entry);
            if(index < 0 && filteredIndex < 0) {
                return false;
            }
            else {
                if(index >= 0) {
                    contacts.remove(index);
                }
                if(filteredIndex >= 0) {
                    filteredContacts.remove(filteredIndex);
                }
                notifyDataSetChanged();
                return true;
            }
        }

        public void add(ContactEntry entry) {
            int index = Collections.binarySearch(contacts, entry);
            if(index < 0) {
                index = ~index;
                contacts.add(index, entry);
                index = Collections.binarySearch(filteredContacts, entry);
                if(index < 0) {
                    index = ~index;
                    filteredContacts.add(index, entry);
                }
                notifyDataSetChanged();
            }
        }

        public boolean contains(ContactEntry entry) {
            return Collections.binarySearch(contacts, entry) >= 0 || Collections.binarySearch(filteredContacts, entry) >= 0;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            final ContactViewHolder holder;

            if(inflater == null) {
                inflater = LayoutInflater.from(SmsActivity.this);
            }

            if(convertView == null) {
                convertView = inflater.inflate(R.layout.layout_contact, null);

                holder = new ContactViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.contact_item_name);
                holder.value = (TextView) convertView.findViewById(R.id.contact_item_number);
                holder.type = (TextView) convertView.findViewById(R.id.contact_item_type);

                if(showNumbers) {
                    holder.value.setVisibility(View.VISIBLE);
                    holder.type.setVisibility(View.VISIBLE);
                }
                else {
                    holder.value.setVisibility(View.GONE);
                    holder.type.setVisibility(View.GONE);
                }
            }
            else {
                holder = (ContactViewHolder) convertView.getTag();
            }

            ContactEntry entry = getItem(i);
            holder.name.setText(entry.getName());
            if(showNumbers) {
                holder.value.setText(entry.getValue());
                holder.type.setText(entry.getType());

            }

            convertView.setTag(holder);

            return convertView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence filterCs) {
                    FilterResults results = new FilterResults();
                    String filter = filterCs.toString().toLowerCase(Locale.ENGLISH);

                    List<ContactEntry> filterList;

                    if(filter == null || filter.trim().isEmpty()) {
                        filterList = null;
                    }
                    else {
                        filterList = new ArrayList<ContactEntry>();
                        for(ContactEntry entry : contacts) {
                            String sanitizedValue = PhoneNumberUtils.extractNetworkPortion(entry.getValue());
                            String sanitizedFilter = PhoneNumberUtils.extractNetworkPortion(filter);
                            if(!sanitizedValue.isEmpty() && !sanitizedFilter.isEmpty() &&
                                    sanitizedValue.startsWith(sanitizedFilter)) {
                                filterList.add(entry);
                            }
                            else {
                                String[] names = entry.getName().split(" ");
                                for(String name : names) {
                                    if(name.toLowerCase(Locale.ENGLISH).startsWith(filter)) {
                                        filterList.add(entry);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    String numberFilter = PhoneNumberUtils.extractNetworkPortion(filter);
                    if(!numberFilter.isEmpty() &&  TextUtils.isDigitsOnly(numberFilter)) {
                        if(filterList == null) {
                            filterList = new ArrayList<ContactEntry>();
                        }
                        filterList.add(new ContactEntry(numberFilter, numberFilter, "Other", false));
                        Collections.sort(filterList);
                    }

                    results.count = (filterList == null ? 0 : filterList.size());
                    results.values = filterList;

                    return results;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    filteredContacts = (ArrayList<ContactEntry>) filterResults.values;
                    if(filteredContacts == null) {
                        filteredContacts = new ArrayList<ContactEntry>(contacts);
                    }
                    notifyDataSetChanged();
                }
            };
        }
    }

    private static class ContactViewHolder {
        TextView name;
        TextView value;
        TextView type;
    }
}
