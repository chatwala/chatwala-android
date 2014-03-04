package com.chatwala.android.activity;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
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
import android.widget.*;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SmsActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String SMS_MESSAGE_URL_EXTRA = "sms_message_url";
    public static final String SMS_MESSAGE_EXTRA = "sms_message";

    private static final int CONTACTS_LOADER_CODE = 0;

    private String smsMessageUrl;
    private String smsMessage;

    private AutoCompleteTextView contactsFilter;
    private ListView recipientsListView;

    private ContactEntryAdapter contactsAdapter;
    private ContactEntryAdapter recipientsAdapter;

    private String[] contactsProjection = new String[] {ContactsContract.CommonDataKinds.Phone._ID,
                                                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                                        ContactsContract.CommonDataKinds.Phone.NUMBER};

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

        ((TextView) findViewById(R.id.sms_message_txt)).setText(smsMessage);

        contactsFilter = (AutoCompleteTextView) findViewById(R.id.contacts_filter);
        contactsFilter.setThreshold(0);
        contactsFilter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ContactEntry entry = contactsAdapter.getItem(i);
                contactsFilter.setText("");
                if(contactsAdapter.remove(entry)) {
                    recipientsAdapter.add(entry);
                    if(recipientsAdapter.getCount() == 1) {
                        findViewById(R.id.recipients_container).setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        recipientsListView = (ListView) findViewById(R.id.recipients_list);
        recipientsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ContactEntry entry = recipientsAdapter.getItem(i);
                if(recipientsAdapter.remove(entry)) {
                    if(entry.isContact()) {
                        contactsAdapter.add(entry);
                    }
                    if(recipientsAdapter.getCount() == 0) {
                        findViewById(R.id.recipients_container).setVisibility(View.GONE);
                    }
                }
            }
        });

        findViewById(R.id.send_sms_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(recipientsAdapter.isEmpty()) {
                    if(noContactsToast != null) {
                        noContactsToast.cancel();
                    }
                    noContactsToast = Toast.makeText(SmsActivity.this, "You must enter recipients", Toast.LENGTH_SHORT);
                    noContactsToast.show();
                }
                else {
                    new SendSmsAsyncTask().execute(new Void[] {});
                }
            }
        });

        getSupportLoaderManager().initLoader(CONTACTS_LOADER_CODE, null, this);
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
        if(!cursor.moveToFirst()) {
            return;
        }

        List<ContactEntry> contacts = new ArrayList<ContactEntry>(cursor.getCount());

        do {
            try {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String value = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                contacts.add(new ContactEntry(name, value, true));
            }
            catch(Exception e) {
                continue;
            }
        } while(cursor.moveToNext());

        contactsAdapter = new ContactEntryAdapter(contacts, true, true);
        contactsFilter.setAdapter(contactsAdapter);

        recipientsAdapter = new ContactEntryAdapter(new ArrayList<ContactEntry>(), false, false);
        recipientsListView.setAdapter(recipientsAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        //do nothing
    }

    private class SendSmsAsyncTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = ProgressDialog.show(SmsActivity.this, "Sending SMS", "Please Wait...");
        }

        @Override
        protected Void doInBackground(Void... v) {
            for(int i = 0; i < recipientsAdapter.getCount(); i++) {
                String message = smsMessage + "... " + smsMessageUrl;
                SmsManager.getDefault().sendTextMessage(recipientsAdapter.getItem(i).getValue(), null, message, null, null);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if(pd != null && pd.isShowing()) {
                pd.dismiss();
            }

            finish();
        }
    }

    private class ContactEntry implements Comparable<ContactEntry> {
        private String name;
        private String value;
        private boolean isContact;

        public ContactEntry(String name, String value, boolean isContact) {
            this.name = name;
            this.value = value;
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

        public boolean isContact() {
            return isContact;
        }

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

                if(showNumbers) {
                    holder.value.setVisibility(View.VISIBLE);
                }
                else {
                    holder.value.setVisibility(View.GONE);
                }
            }
            else {
                holder = (ContactViewHolder) convertView.getTag();
            }

            ContactEntry entry = getItem(i);
            holder.name.setText(entry.getName());
            if(showNumbers) {
                holder.value.setText(entry.getValue());
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
                            if(entry.getName().toLowerCase(Locale.ENGLISH).startsWith(filter) ||
                                    entry.getValue().toLowerCase(Locale.ENGLISH).startsWith(filter)) {
                                filterList.add(entry);
                            }
                        }
                    }

                    String numberFilter = PhoneNumberUtils.extractNetworkPortion(filter);
                    if(!numberFilter.isEmpty() &&  TextUtils.isDigitsOnly(numberFilter)) {
                        if(filterList == null) {
                            filterList = new ArrayList<ContactEntry>();
                        }
                        filterList.add(new ContactEntry(numberFilter, numberFilter, false));
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
    }
}
