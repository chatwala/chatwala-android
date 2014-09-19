package com.chatwala.android.contacts;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.content.AsyncTaskLoader;
import com.chatwala.android.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 5:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class ContactsLoader extends AsyncTaskLoader<List<ContactEntry>> {
    private List<ContactEntry> contacts;

    private String[] projection = new String[] {ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI};

    public ContactsLoader(Context context) {
        super(context);
        onContentChanged();
    }

    @Override
    protected void onStartLoading() {
        if(contacts != null) {
            deliverResult(contacts);
        }

        if(contacts == null || takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public List<ContactEntry> loadInBackground() {
        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY);

            if(cursor != null) {
                contacts = new ArrayList<ContactEntry>(cursor.getCount());
                if (cursor.moveToFirst()) {
                    do {
                        try {
                            String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            String value = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            String type = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE));
                            type = getTypeString(type);
                            String image = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                            contacts.add(new ContactEntry(name, value, type, image, true));
                        } catch (Exception e) {
                            continue;
                        }
                    } while (cursor.moveToNext());
                }
            }

            return contacts;
        }
        catch(Exception e) {
            Logger.e("Got an error loading the contacts", e);
            return new ArrayList<ContactEntry>(0);
        }
        finally {
            if(cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
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
    public void deliverResult(List<ContactEntry> contacts) {
        if(isReset()) {
            if(contacts != null) {
                onReleaseResources(contacts);
            }
        }
        List<ContactEntry> oldContacts = this.contacts;
        this.contacts = contacts;

        if(isStarted()) {
            super.deliverResult(contacts);
        }

        if(oldContacts != null) {
            onReleaseResources(oldContacts);
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(List<ContactEntry> contacts) {
        super.onCanceled(contacts);

        onReleaseResources(contacts);
    }

    @Override
    protected void onReset() {
        super.onReset();

        onStopLoading();

        if(contacts != null) {
            onReleaseResources(contacts);
        }
    }

    private void onReleaseResources(List<ContactEntry> contacts) {
        contacts = null;
    }
}

