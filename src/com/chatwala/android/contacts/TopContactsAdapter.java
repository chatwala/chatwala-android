package com.chatwala.android.contacts;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.chatwala.android.R;
import com.chatwala.android.util.Logger;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eliezer on 4/1/2014.
 */
public class TopContactsAdapter extends ContactsAdapter {
    private ArrayList<String> contactsToSendTo;
    private Picasso pic;

    public interface TopContactsEventListener {
        public void onContactRemoved(int contactsLeft);
        public void onContactClicked();
        public void onSend();
    }

    private TopContactsEventListener listener;

    public TopContactsAdapter(Context context, List<ContactEntry> contacts, boolean useFiltered, TopContactsEventListener listener) {
        super(context, contacts, useFiltered, null);
        try {
            pic = Picasso.with(getContext());
        }
        catch(Exception e){
            Logger.e("Couldn't load Picasso for the top contacts adapter", e);
        }
        this.listener = listener;

        contactsToSendTo = new ArrayList<String>(contacts.size());
        for(ContactEntry contact : contacts) {
            contactsToSendTo.add(contact.getValue());
        }
    }

    public ArrayList<String> getContactsToSendTo() {
        return contactsToSendTo;
    }

    public void clearContactsToSendTo() {
        contactsToSendTo.clear();
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if(getLayoutInflater() == null) {
            setLayoutInflater(getContext());
        }

        if(convertView == null) {
            convertView = getLayoutInflater().inflate(R.layout.layout_top_contacts_grid, null);

            holder = new ViewHolder();
            holder.overlay = convertView.findViewById(R.id.contact_item_overlay);
            holder.name = (TextView) convertView.findViewById(R.id.contact_item_name);
            holder.value = (TextView) convertView.findViewById(R.id.contact_item_number);
            holder.check = (CheckBox) convertView.findViewById(R.id.contact_check);
            holder.image = (ImageView) convertView.findViewById(R.id.contact_item_image);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        final ContactEntry entry = getItem(i);
        String[] names = entry.getName().split(" ");
        if(names.length > 0) {
            holder.name.setText(names[0]);
        }
        holder.value.setText(entry.getValue());

        try {
            pic.load(entry.getImage())
                    .error(R.drawable.default_contact_icon)
                    .placeholder(R.drawable.default_contact_icon)
                    .noFade()
                    .into(holder.image);
        }
        catch(Exception e){
            Logger.e("Error loading the contacts image", e);
        }

        holder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    contactsToSendTo.add(entry.getValue());
                }
                else {
                    contactsToSendTo.remove(entry.getValue());
                    listener.onContactRemoved(contactsToSendTo.size());
                }
            }
        });
        convertView.setTag(holder);

        return convertView;
    }

    private static class ViewHolder {
        View overlay;
        TextView name;
        TextView value;
        CheckBox check;
        ImageView image;
    }
}
