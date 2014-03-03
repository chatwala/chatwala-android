package com.chatwala.android.activity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chatwala.android.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SmsActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String SMS_MESSAGE_EXTRA = "sms_message";

    private static final int CONTACTS_LOADER_CODE = 0;

    private String smsMessage;

    private EditText contactsFilter;
    private ListView contactsListView;
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
        setContentView(R.layout.activity_sms);

        if(!getIntent().hasExtra(SMS_MESSAGE_EXTRA)) {
            finish();
            return;
        }

        smsMessage = getIntent().getStringExtra(SMS_MESSAGE_EXTRA);

        contactsFilter = (EditText) findViewById(R.id.contacts_filter);
        contactsFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable s) {
                contactsFilter.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        contactsAdapter.getFilter().filter(s.toString());
                    }
                }, 300);
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
        });

        contactsListView = (ListView) findViewById(R.id.contacts_list);

        recipientsListView = (ListView) findViewById(R.id.recipients_list);
        recipientsListView.setEmptyView(findViewById(R.id.recipients_empty));

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ContactEntry entry = contactsAdapter.getItem(i);
                if(contactsAdapter.remove(entry)) {
                    recipientsAdapter.add(entry);
                }
            }
        });

        recipientsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ContactEntry entry = recipientsAdapter.getItem(i);
                if(recipientsAdapter.remove(entry)) {
                    contactsAdapter.add(entry);
                }
            }
        });

        findViewById(R.id.send_sms_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(recipientsAdapter.getCount() <= 0) {
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
                contacts.add(new ContactEntry(name, value));
            }
            catch(Exception e) {
                continue;
            }
        } while(cursor.moveToNext());

        contactsAdapter = new ContactEntryAdapter(contacts, true);
        contactsListView.setAdapter(contactsAdapter);

        recipientsAdapter = new ContactEntryAdapter(new ArrayList<ContactEntry>(), false);
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
                SmsManager.getDefault().sendTextMessage(recipientsAdapter.getItem(i).getValue(), null, smsMessage, null, null);
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

        public ContactEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
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

            int compare = getName().compareToIgnoreCase(other.getName());
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
        private LayoutInflater inflater;

        public ContactEntryAdapter(List<ContactEntry> contacts, boolean useFiltered) {
            this.contacts = contacts;
            Collections.sort(this.contacts);
            filteredContacts = new ArrayList<ContactEntry>(this.contacts);
            this.useFiltered = useFiltered;
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
            if(index < 0) {
                return false;
            }
            else {
                contacts.remove(index);
                index = Collections.binarySearch(filteredContacts, entry);
                if(index >= 0) {
                    filteredContacts.remove(index);
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
            return Collections.binarySearch(contacts, entry) >= 0;
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
                holder.name = (TextView) convertView.findViewById((R.id.contact_item_name));
                holder.value = (TextView) convertView.findViewById((R.id.contact_item_number));
            }
            else {
                holder = (ContactViewHolder) convertView.getTag();
            }

            ContactEntry entry = getItem(i);
            holder.name.setText(entry.getName());
            holder.value.setText(entry.getValue());

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

                    if(filter == null || filter.trim().isEmpty()) {
                        filteredContacts = new ArrayList<ContactEntry>(contacts);
                    }
                    else {
                        filteredContacts = new ArrayList<ContactEntry>();
                        for(ContactEntry entry : contacts) {
                            if(entry.getName().toLowerCase(Locale.ENGLISH).startsWith(filter) ||
                                    entry.getValue().toLowerCase(Locale.ENGLISH).startsWith(filter)) {
                                filteredContacts.add(entry);
                            }
                        }
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
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
