package com.chatwala.android.contacts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.content.AsyncTaskLoader;
import android.telephony.PhoneNumberUtils;
import com.chatwala.android.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Eliezer on 3/31/2014.
 */
public class FrequentContactsLoader extends AsyncTaskLoader<List<ContactEntry>> {
    private List<ContactEntry> contacts;
    private int howManyContactsToLoad;
    private int offset;

    @SuppressLint("InlinedApi")
    private static final String LAST_TIME_CONTACTED = (isApi18OrGreater() ?
            ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED : ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED);
    @SuppressLint("InlinedApi")
    private static final String TIMES_CONTACTED = (isApi18OrGreater() ?
            ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED : ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED);

    private static final boolean isApi18OrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    private String[] projection = new String[] {ContactsContract.CommonDataKinds.Phone._ID,
            TIMES_CONTACTED,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI};

    public FrequentContactsLoader(Context context, int howManyContactsToLoad) {
        this(context, howManyContactsToLoad, 0);
    }

    public FrequentContactsLoader(Context context, int howManyContactsToLoad, int offset) {
        super(context);
        this.howManyContactsToLoad = howManyContactsToLoad;
        this.offset = offset;
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

    private static final long THIRTY_DAYS_MS = 2592000000L;
    @Override
    public List<ContactEntry> loadInBackground() {
        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER + "=1 AND " +
                            LAST_TIME_CONTACTED + " >= " + (System.currentTimeMillis() - THIRTY_DAYS_MS),
                    null,
                    TIMES_CONTACTED + " DESC, " + LAST_TIME_CONTACTED + " ASC");

            contacts = new ArrayList<ContactEntry>(howManyContactsToLoad);
            Map<String, FrequentContactEntry> nonMobileRecentContacts = new HashMap<String, FrequentContactEntry>();
            Map<String, Boolean> addToRecents = new HashMap<String, Boolean>();
            Map<String, String> addToRecentsByNumber = new HashMap<String, String>();
            String previousName = null;

            int offsetCounter = 0;
            if(cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        if(offsetCounter++ < offset) {
                            continue;
                        }
                        try {
                            String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            String value = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            String normalizedValue = PhoneNumberUtils.extractNetworkPortion(value).replace("+", "").trim();
                            if (normalizedValue.startsWith("1") && normalizedValue.length() > 1) {
                                normalizedValue = normalizedValue.substring(1);
                            }
                            String type = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE));
                            String image = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                            int timesContacted = cursor.getInt(cursor.getColumnIndexOrThrow(TIMES_CONTACTED));
                            type = getTypeString(type);

                            if (previousName == null) {
                                previousName = name;
                            }

                            if (addToRecentsByNumber.containsKey(normalizedValue) &&
                                    PhoneNumberUtils.compare(normalizedValue, addToRecentsByNumber.get(normalizedValue))) {
                                continue;
                            }

                            if (!"Mobile".equals(type)) {
                                if (!addToRecents.containsKey(name) && //if this name wasn't encountered yet
                                        addToRecents.containsKey(previousName) && //we should add the previous name to list
                                        addToRecents.get(previousName)) {
                                    if (nonMobileRecentContacts.containsKey(previousName)) {
                                        FrequentContactEntry previousNameEntry = nonMobileRecentContacts.get(previousName);
                                        String normalizedPreviousValue = PhoneNumberUtils.extractNetworkPortion(previousNameEntry.getValue());
                                        if (!addToRecentsByNumber.containsKey(normalizedPreviousValue)) {
                                            addToRecentsByNumber.put(normalizedPreviousValue, previousNameEntry.getValue()); //don't use this number again
                                            contacts.add(previousNameEntry);
                                            if (contacts.size() == howManyContactsToLoad) {
                                                break;
                                            }
                                        }
                                    }
                                    previousName = name;
                                    nonMobileRecentContacts.put(name, new FrequentContactEntry(name, value, type, image, timesContacted, true));
                                    addToRecents.put(name, true); //any subsequent equal names won't hit this block
                                } else if (addToRecents.get(name) != null) { //there is more than one non-mobile
                                    addToRecents.put(name, false);
                                }
                            } else {
                                addToRecents.put(name, false); //we have mobile; don't add any non-mobile
                                addToRecentsByNumber.put(normalizedValue, value); //don't use this number again
                                contacts.add(new FrequentContactEntry(name, value, type, image, timesContacted, true));
                            }
                        } catch (Exception e) {
                            Logger.e("Exception", e);
                            continue;
                        }
                    } while (cursor.moveToNext() && contacts.size() != howManyContactsToLoad);
                }
            }

            return contacts;
        }
        catch(Exception e) {
            Logger.e("Got an error loading the frequent contacts", e);
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
