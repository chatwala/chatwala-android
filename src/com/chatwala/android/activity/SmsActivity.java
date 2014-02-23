package com.chatwala.android.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import com.chatwala.android.R;

public class SmsActivity extends Activity {
    public static final String SMS_MESSAGE_EXTRA = "sms_message";

    private static final int CONTACTS_REQUEST_CODE = 0;

    private EditText smsMessageBox;
    private EditText contactsBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        if(!getIntent().hasExtra(SMS_MESSAGE_EXTRA)) {
            finish();
            return;
        }

        String message = getIntent().getStringExtra(SMS_MESSAGE_EXTRA);
        smsMessageBox = (EditText) findViewById(R.id.message_text);
        smsMessageBox.setText(message);

        contactsBox = (EditText) findViewById(R.id.add_contacts_box);

        findViewById(R.id.add_contact_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(intent, CONTACTS_REQUEST_CODE);
            }
        });

        findViewById(R.id.send_sms_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSms();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            if(requestCode == CONTACTS_REQUEST_CODE) {
                Uri uri = data.getData();

                if(uri != null) {
                    Cursor c = null;
                    String nameCol = ContactsContract.Contacts.DISPLAY_NAME;
                    String numberCol = ContactsContract.CommonDataKinds.Phone.NUMBER;
                    try {
                        c = getContentResolver().query(uri, new String[] {nameCol, numberCol},
                                null, null, null);
                        if(c != null && c.moveToFirst()) {
                            int nameIndex = c.getColumnIndex(nameCol);
                            int numberIndex = c.getColumnIndex(numberCol);
                            if(numberIndex != -1) {
                                String name =  null;
                                try {
                                    name = c.getString(nameIndex);
                                }
                                catch(Exception ignore) {}
                                String number = c.getString(numberIndex);

                                if(!contactsBox.getText().toString().trim().isEmpty()) {
                                    contactsBox.append("; ");
                                }

                                if(name != null) {
                                    contactsBox.append("<" + name + ">");
                                }

                                contactsBox.append(number);
                            }
                        }
                    }
                    finally {
                        if(c != null && !c.isClosed()) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    private void sendSms() {
        String[] addresses = contactsBox.getText().toString().split(";");
        new SendSmsAsyncTask().execute(addresses);
    }

    private int getIndexOfLastBracket(String s) {
        int bracketIndex = 0;
        for(int i = 0; i < s.length(); i++) {
            if(s.charAt(i) == '>') {
                bracketIndex = i;
            }
        }
        return bracketIndex;
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
            for(String address : addresses) {
                int lastBracketIndex = getIndexOfLastBracket(address);
                if(lastBracketIndex > 0) {
                    address = address.substring(getIndexOfLastBracket(address) + 1).trim();
                }
                SmsManager.getDefault().sendTextMessage(address, null, smsMessageBox.getText().toString(), null, null);
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
}
