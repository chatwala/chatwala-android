package com.chatwala.android.contacts;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import com.chatwala.android.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by Eliezer on 3/31/2014.
 */
public class ContactsAdapter extends BaseAdapter implements Filterable, ContactEntry.OnSendStateChangedListener {
    private Context context;
    private List<ContactEntry> contacts;
    private List<ContactEntry> filteredContacts;
    private boolean useFiltered;
    private LayoutInflater inflater;
    protected OnItemCheckedChangeListener itemCheckedChangeListener;

    public ContactsAdapter(Context context, List<ContactEntry> contacts, boolean useFiltered) {
        this.context = context;
        this.contacts = contacts;
        Collections.sort(this.contacts);
        filteredContacts = new ArrayList<ContactEntry>(this.contacts);
        this.useFiltered = useFiltered;
        inflater = LayoutInflater.from(context);
    }

    public interface OnItemCheckedChangeListener {
        public void onItemCheckedChanged(ContactEntry contact, boolean isChecked);
    }

    public void setOnItemCheckedChangeListener(OnItemCheckedChangeListener itemCheckedChangeListener) {
        this.itemCheckedChangeListener = itemCheckedChangeListener;
    }

    protected OnItemCheckedChangeListener getItemCheckedChangeListener() {
        return itemCheckedChangeListener;
    }

    public void setItemOnSendStateChangedListener(ContactEntry contact) {
        contact.setOnSendStateChangedListener(this);
    }

    protected LayoutInflater getLayoutInflater() {
        return inflater;
    }

    protected void setLayoutInflater(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public List<ContactEntry> getList() {
        if(useFiltered) {
            return filteredContacts;
        }
        else {
            return contacts;
        }
    }

    protected Context getContext() {
        return context;
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

    public ContactEntry find(ContactEntry entry) {
        int index = Collections.binarySearch(getList(), entry);
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
        final ViewHolder holder;

        if(getLayoutInflater() == null) {
            setLayoutInflater(getContext());
        }

        if(convertView == null) {
            convertView = getLayoutInflater().inflate(R.layout.layout_contact, null);

            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.contact_item_name);
            holder.value = (TextView) convertView.findViewById(R.id.contact_item_number);
            holder.type = (TextView) convertView.findViewById(R.id.contact_item_type);
            holder.status = (TextView) convertView.findViewById(R.id.contact_sent_status);
            holder.sentCb = (CheckBox) convertView.findViewById(R.id.contact_sent_cb);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        final ContactEntry entry = getItem(i);
        holder.name.setText(entry.getName());
        holder.value.setText(entry.getValue());
        holder.type.setText(entry.getType());
        holder.status.setText(entry.getSendingStatus());
        holder.sentCb.setChecked(entry.isSentOrSending());
        holder.sentCb.setEnabled(!entry.isSent());
        holder.sentCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = ((CheckBox)v).isChecked();
                if(getItemCheckedChangeListener() != null) {
                    getItemCheckedChangeListener().onItemCheckedChanged(entry, isChecked);
                }
            }
        });
        int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
        if(id != 0) {
            holder.sentCb.setButtonDrawable(id);
        }

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
                    filterList.add(new ContactEntry(numberFilter, numberFilter, "Other", null, false));
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

    @Override
    public void onSendStateChanged() {
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView name;
        TextView value;
        TextView type;
        TextView status;
        CheckBox sentCb;
    }
}
