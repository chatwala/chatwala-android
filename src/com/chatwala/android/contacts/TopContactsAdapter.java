package com.chatwala.android.contacts;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.chatwala.android.R;
import com.chatwala.android.ui.CWButton;
import com.chatwala.android.util.Logger;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eliezer on 4/1/2014.
 */
public class TopContactsAdapter extends ContactsAdapter {
    private Picasso pic;

    public interface TopContactsEventListener {
        public void onContactRemoved(int contactsLeft);
        public void onContactClicked();
        public void onSend();
    }

    private TopContactsEventListener listener;

    public TopContactsAdapter(Context context, List<ContactEntry> contacts, boolean useFiltered, TopContactsEventListener listener) {
        super(context, contacts, useFiltered);
        try {
            pic = Picasso.with(getContext());
        }
        catch(Exception e){
            Logger.e("Couldn't load Picasso for the top contacts adapter", e);
        }
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return 9;
    }

    public ArrayList<String> getContactsToSendTo() {
        ArrayList<String> contacts = new ArrayList<String>(getList().size());
        for(ContactEntry contact : getList()) {
            contacts.add(contact.getValue());
        }
        return contacts;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if(getLayoutInflater() == null) {
            setLayoutInflater(getContext());
        }

        if(i == 4) {
            convertView = getLayoutInflater().inflate(R.layout.layout_top_contacts_cw_button, null);
            if(convertView == null) {
                convertView = new CWButton(getContext());
            }
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onSend();
                    }
                }
            });
            convertView.setTag(null);
            return convertView;
        }
        else if(i > 4) {
            i--;
        }

        if(convertView == null || convertView.getTag() == null) {
            convertView = getLayoutInflater().inflate(R.layout.layout_top_contacts_grid, null);

            holder = new ViewHolder();
            holder.overlay = convertView.findViewById(R.id.contact_item_overlay);
            holder.name = (TextView) convertView.findViewById(R.id.contact_item_name);
            holder.value = (TextView) convertView.findViewById(R.id.contact_item_number);
            holder.xOut = (TextView) convertView.findViewById(R.id.contact_x_out);
            holder.image = (ImageView) convertView.findViewById(R.id.contact_item_image);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        if(i >= getList().size()) {
            convertView.setVisibility(View.GONE);
            return convertView;
        }
        else {
            convertView.setVisibility(View.VISIBLE);
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

        holder.xOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove(entry);

                if(listener != null) {
                    listener.onContactRemoved(getList().size());
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
        TextView xOut;
        ImageView image;
    }
}
