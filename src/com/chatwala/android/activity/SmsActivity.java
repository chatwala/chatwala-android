package com.chatwala.android.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.chatwala.android.R;
import com.chatwala.android.SmsSentReceiver;
import com.chatwala.android.util.CWAnalytics;
import com.chatwala.android.util.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class SmsActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String SMS_MESSAGE_URL_EXTRA = "sms_message_url";
    public static final String SMS_MESSAGE_EXTRA = "sms_message";

    private static final int MAX_SMS_MESSAGE_LENGTH = 160;

    private static final int MOST_CONTACTED_CONTACT_LIMIT = 25;

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

    private ContactEntryAdapter contactsAdapter;
    private ContactEntryAdapter mostContactedAdapter;

    private String[] contactsProjection = new String[] {ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE};

    private String[] contactedTimesProjection = new String[] {ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE};

    private Comparator<ContactEntry> entryComparator = new Comparator<ContactEntry>() {

        @Override
        public int compare(ContactEntry me, ContactEntry other) {
            if(other == null) {
                return -1;
            }

            String name = me.getName();
            String otherName = other.getName();

            if(!name.isEmpty() && Character.isDigit(name.charAt(0))) {
                name = "_" + name;
            }

            if(!otherName.isEmpty() && Character.isDigit(otherName.charAt(0))) {
                otherName = "_" + otherName;
            }

            int compare = name.compareToIgnoreCase(otherName);
            if(compare == 0) {
                return me.getValue().compareToIgnoreCase(other.getValue());
            }
            else {
                return compare;
            }
        }
    };

    private Comparator<ContactEntry> mostContactedEntryComparator = new Comparator<ContactEntry>() {

        @Override
        public int compare(ContactEntry me, ContactEntry other) {
            MostContactedContactEntry mostContactedMe = (MostContactedContactEntry) me;
            MostContactedContactEntry mostContactedOther = (MostContactedContactEntry) other;
            if(mostContactedMe.getTimesContacted() > mostContactedOther.getTimesContacted()) {
                return -1;
            }
            else if(mostContactedMe.getTimesContacted() == mostContactedOther.getTimesContacted()) {
                return 0;
            }
            else {
                return 1;
            }
        }
    };

    private class SendMessageRunnable implements Runnable {
        private String value;

        public SendMessageRunnable(String value) {
            this.value = value;
        }

        @Override
        public void run() {
            String messageUrlWithPrefix = "... " + smsMessageUrl;
            int maxMessageSize = MAX_SMS_MESSAGE_LENGTH - messageUrlWithPrefix.length();
            if(smsMessage.length() > maxMessageSize) {
                smsMessage = smsMessage.substring(maxMessageSize - 1) + messageUrlWithPrefix;
            }
            String message = smsMessage + "... " + smsMessageUrl;
            PendingIntent sentIntent = PendingIntent.getBroadcast(SmsActivity.this, hashCode(),
                    new Intent(SmsActivity.this, SmsSentReceiver.class), 0);
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

        contactsAdapter = new ContactEntryAdapter(new ArrayList<ContactEntry>(), true, entryComparator);
        mostContactedAdapter = new ContactEntryAdapter(new ArrayList<ContactEntry>(), false, mostContactedEntryComparator);

        contactsFilter = (EditText) findViewById(R.id.contacts_filter);
        contactsFilter.setHintTextColor(Color.WHITE);
        contactsFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                //TODO put in postDelayed if not working
                if(s.toString().isEmpty()) {
                    contactsListView.setAdapter(mostContactedAdapter);
                    findViewById(R.id.recent_contacts_lbl).setVisibility(View.VISIBLE);
                }
                else {
                    contactsListView.setAdapter(contactsAdapter);
                    findViewById(R.id.recent_contacts_lbl).setVisibility(View.GONE);
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
                ContactEntryAdapter adapter = (ContactEntryAdapter) contactsListView.getAdapter();
                ContactEntry entry = adapter.getItem(i);
                if(entry.isSending()) {
                    contactsSentTo.remove(entry.getName() + entry.getValue());
                    entry.cancelSend();
                }
                else {
                    if(contactsSentTo.containsKey(entry.getName() + entry.getValue())) {
                        entry.setIsSent(true);
                    }
                    else {
                        contactsSentTo.put(entry.getName() + entry.getValue(), true);
                        entry.startSend();
                    }
                }
            }
        });

        getSupportLoaderManager().initLoader(CONTACTS_LOADER_CODE, null, this);
        getSupportLoaderManager().initLoader(CONTACTS_TIME_CONTACTED_LOADER_CODE, null, this);
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
        else if(loaderCode == CONTACTS_TIME_CONTACTED_LOADER_CODE) {
            return new CursorLoader(SmsActivity.this,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    contactedTimesProjection,
                    ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER + "=1",
                    null,
                    ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED + " DESC, " +
                            ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED + " ASC");
        }
        else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if(cursorLoader.getId() == CONTACTS_LOADER_CODE) {
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

            contactsAdapter = new ContactEntryAdapter(contacts, true, entryComparator);
        }
        else if(cursorLoader.getId() == CONTACTS_TIME_CONTACTED_LOADER_CODE) {
            List<ContactEntry> mostContactedContacts = new ArrayList<ContactEntry>(MOST_CONTACTED_CONTACT_LIMIT);
            Map<String, MostContactedContactEntry> nonMobileRecentContacts = new HashMap<String, MostContactedContactEntry>();
            Map<String, Boolean> addToRecents = new HashMap<String, Boolean>();
            String previousName = null;

            if(cursor.moveToFirst()) {
                while(cursor.moveToNext() && mostContactedContacts.size() != MOST_CONTACTED_CONTACT_LIMIT) {
                    try {
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        String value = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String type = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE));
                        int timesContacted = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED));
                        type = getTypeString(type);

                        if(previousName == null) {
                            previousName = name;
                        }

                        if(!"Mobile".equals(type)) {
                            if(!addToRecents.containsKey(name) && //if this name wasn't encountered yet
                               addToRecents.containsKey(previousName) && //we should add the previous name to list
                               addToRecents.get(previousName)) {
                                if(nonMobileRecentContacts.containsKey(previousName)) {
                                    mostContactedContacts.add(nonMobileRecentContacts.get(previousName));
                                    if(mostContactedContacts.size() == MOST_CONTACTED_CONTACT_LIMIT) {
                                        break;
                                    }

                                }
                                previousName = name;
                                nonMobileRecentContacts.put(name, new MostContactedContactEntry(name, value, type, timesContacted, true));
                                addToRecents.put(name, true);
                            }
                            else if(addToRecents.get(name) != null) { //there is more than one non-mobile
                                addToRecents.put(name, false);
                            }
                        }
                        else {
                            addToRecents.put(name, false); //we have mobile; don't add any non-mobile
                            mostContactedContacts.add(new MostContactedContactEntry(name, value, type, timesContacted, true));
                        }
                    }
                    catch(Exception e) {
                        Logger.e("Exceptione", e);
                        continue;
                    }
                }
            }

            mostContactedAdapter = new ContactEntryAdapter(mostContactedContacts, true, mostContactedEntryComparator);
            contactsListView.setAdapter(mostContactedAdapter);
        }
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

    private class ContactEntry {
        private String name;
        private String value;
        private String type;
        private boolean isContact;

        private boolean isSending = false;
        private boolean isSent = false;
        private static final int TIME_TO_SEND = 5;
        private int timeToSend = TIME_TO_SEND;

        private Handler sendingHandler = new Handler();

        private Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                timeToSend--;
                if(timeToSend == 0) {
                    sendMessage();
                }
                else {
                    if(isSending()) {
                        sendingHandler.postDelayed(this, 1000);
                    }
                }
                contactsAdapter.notifyDataSetChanged();
                mostContactedAdapter.notifyDataSetChanged();
            }
        };

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

        public String getType() {
            return (type == null ? "" : type);
        }

        public boolean isContact() { return isContact; }

        public boolean equals(ContactEntry other) {
            return entryComparator.compare(this, other) == 0;
        }

        public boolean isSending() {
            return isSending;
        }

        public boolean isSent() {
            return isSent;
        }

        public void setIsSent(boolean isSent) {
            this.isSent = isSent;
        }

        public boolean isSentOrSending() {
            return isSending || isSent;
        }

        public boolean startSend() {
            if(isSent) {
                return false;
            }

            isSending = true;
            timeToSend = TIME_TO_SEND;

            sendingHandler.post(countdownRunnable);
            return true;
        }

        public void cancelSend() {
            sendingHandler.removeCallbacks(countdownRunnable);
            isSending = false;
            isSent = false;
            timeToSend = TIME_TO_SEND;
            contactsAdapter.notifyDataSetChanged();
            mostContactedAdapter.notifyDataSetChanged();
        }

        public String getSendingStatus() {
            if(isSent) {
                return "Sent";
            }
            else if(isSending) {
                return "Sending in " + timeToSend;
            }
            else {
                return "";
            }
        }

        private void sendMessage() {
            isSending = false;
            isSent = true;
            executor.execute(new SendMessageRunnable(getValue()));
        }

        public int hashCode() {
            return (getName() + getValue()).hashCode();
        }
    }

    private class MostContactedContactEntry extends ContactEntry {
        private int timesContacted;

        public MostContactedContactEntry(String name, String value, String type, int timesContacted, boolean isContact) {
            super(name, value, type, isContact);
            this.timesContacted = timesContacted;
        }

        public int getTimesContacted() { return timesContacted; }

        public boolean equals(ContactEntry other) {
            return mostContactedEntryComparator.compare(this, other) == 0;
        }
    }

    private class ContactEntryAdapter extends BaseAdapter implements Filterable {
        private List<ContactEntry> contacts;
        private List<ContactEntry> filteredContacts;
        private boolean useFiltered;
        private LayoutInflater inflater;
        private Comparator<ContactEntry> comparator;

        public ContactEntryAdapter(List<ContactEntry> contacts, boolean useFiltered, Comparator<ContactEntry> comparator) {
            this.contacts = contacts;
            Collections.sort(this.contacts, comparator);
            filteredContacts = new ArrayList<ContactEntry>(this.contacts);
            this.useFiltered = useFiltered;
            inflater = LayoutInflater.from(SmsActivity.this);
            this.comparator = comparator;
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
            int index = Collections.binarySearch(contacts, entry, comparator);
            int filteredIndex = Collections.binarySearch(filteredContacts, entry, comparator);
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
            int index = Collections.binarySearch(contacts, entry, comparator);
            if(index < 0) {
                index = ~index;
                contacts.add(index, entry);
                index = Collections.binarySearch(filteredContacts, entry, comparator);
                if(index < 0) {
                    index = ~index;
                    filteredContacts.add(index, entry);
                }
                notifyDataSetChanged();
            }
        }

        public boolean contains(ContactEntry entry) {
            return Collections.binarySearch(contacts, entry, comparator) >= 0 || Collections.binarySearch(filteredContacts, entry, comparator) >= 0;
        }

        public ContactEntry find(ContactEntry entry) {
            int index = Collections.binarySearch(getList(), entry, comparator);
            if(index >= 0) {
                return getItem(index);
            }
            else {
                return null;
            }
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
                holder.status = (TextView) convertView.findViewById(R.id.contact_sent_status);
                holder.sentCb = (CheckBox) convertView.findViewById(R.id.contact_sent_cb);
            }
            else {
                holder = (ContactViewHolder) convertView.getTag();
            }

            final ContactEntry entry = getItem(i);
            holder.name.setText(entry.getName());
            holder.value.setText(entry.getValue());
            holder.type.setText(entry.getType());
            holder.status.setText(entry.getSendingStatus());
            holder.sentCb.setChecked(entry.isSentOrSending());
            holder.sentCb.setEnabled(!entry.isSent());

            if(entry.isSent()) {
                convertView.setBackgroundColor(Color.GRAY);
            }
            else {
                convertView.setBackgroundColor(Color.WHITE);
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
                                if(entry.getName().toLowerCase((Locale.ENGLISH)).startsWith(filter)) {
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
                    }

                    String numberFilter = PhoneNumberUtils.extractNetworkPortion(filter);
                    if(!numberFilter.isEmpty() &&  TextUtils.isDigitsOnly(numberFilter)) {
                        if(filterList == null) {
                            filterList = new ArrayList<ContactEntry>();
                        }
                        filterList.add(new ContactEntry(numberFilter, numberFilter, "Other", false));
                        Collections.sort(filterList, comparator);
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
        TextView status;
        CheckBox sentCb;
    }
}
