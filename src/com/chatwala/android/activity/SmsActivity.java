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
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import com.chatwala.android.R;

public class SmsActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String SMS_MESSAGE_EXTRA = "sms_message";

    private static final int CONTACTS_LOADER_CODE = 0;

    private EditText contactsFilter;
    private ListView contactsList;

    private CursorAdapter adapter;

    private String[] contactsProjection = new String[] {ContactsContract.CommonDataKinds.Phone._ID,
                                                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                                        ContactsContract.CommonDataKinds.Phone.NUMBER};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        if(!getIntent().hasExtra(SMS_MESSAGE_EXTRA)) {
            finish();
            return;
        }

        //String message = getIntent().getStringExtra(SMS_MESSAGE_EXTRA);

        contactsFilter = (EditText) findViewById(R.id.contacts_filter);
        contactsFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable s) {
                contactsFilter.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        adapter.getFilter().filter(s.toString());
                    }
                }, 300);
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
        });

        contactsList = (ListView) findViewById(R.id.contacts_list);
        adapter = new SimpleCursorAdapter(this,
                                          R.layout.layout_contact,
                                          null,
                                          new String[] { ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER },
                                          new int[] { R.id.contact_item_name, R.id.contact_item_number },
                                          0);
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence filter) {
                filter = DatabaseUtils.sqlEscapeString(filter.toString() + "%");
                return getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                                  contactsProjection,
                                                  ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE " + filter + " OR " +
                                                  ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE " + filter,
                                                  null,
                                                  ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY);
            }
        });
        contactsList.setAdapter(adapter);

        contactsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });

        findViewById(R.id.send_sms_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSms();
            }
        });

        getSupportLoaderManager().initLoader(CONTACTS_LOADER_CODE, null, this);
    }

    private void sendSms() {
        //String[] addresses = contactsBox.getText().toString().split(";");
        //new SendSmsAsyncTask().execute(addresses);
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
        adapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        adapter.changeCursor(null);
    }

    private class SendSmsAsyncTask extends AsyncTask<String, Void, Void> {
        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = ProgressDialog.show(SmsActivity.this, "Sending SMS", "Please Wait...");
        }

        @Override
        protected Void doInBackground(String... addresses) {
            /*for(String address : addresses) {
                int lastBracketIndex = getIndexOfLastBracket(address);
                if(lastBracketIndex > 0) {
                    address = address.substring(getIndexOfLastBracket(address) + 1).trim();
                }
                SmsManager.getDefault().sendTextMessage(address, null, smsMessageBox.getText().toString(), null, null);
            }*/

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
}
