package com.chatwala.android.contacts;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.chatwala.android.R;
import com.chatwala.android.util.Logger;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by Eliezer on 3/31/2014.
 */
public class FrequentContactsAdapter extends ContactsAdapter {
    private Picasso pic;

    public FrequentContactsAdapter(Context context, List<ContactEntry> contacts, boolean useFiltered, OnContactActionListener listener) {
        super(context, contacts, useFiltered, listener);
        try {
            pic = Picasso.with(getContext());
        }
        catch(Exception e){
            Logger.e("Couldn't load Picasso for the frequent contacts adapter", e);
        }
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if(getLayoutInflater() == null) {
            setLayoutInflater(getContext());
        }

        if(convertView == null) {
            convertView = getLayoutInflater().inflate(R.layout.layout_contact_grid, null);

            holder = new ViewHolder();
            holder.overlay = convertView.findViewById(R.id.contact_item_overlay);
            holder.name = (TextView) convertView.findViewById(R.id.contact_item_name);
            holder.value = (TextView) convertView.findViewById(R.id.contact_item_number);
            holder.status = (TextView) convertView.findViewById(R.id.contact_sent_status);
            holder.sentCb = (CheckBox) convertView.findViewById(R.id.contact_sent_cb);
            holder.image = (ImageView) convertView.findViewById(R.id.contact_item_image);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        final ContactEntry contact = getItem(i);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getOnContactActionListener() != null) {
                    if(contact.isSending()) {
                        getOnContactActionListener().onSendCanceled(contact);
                    }
                    else if(!contact.isSent()) {
                        getOnContactActionListener().onStartSend(contact);
                    }
                    else {
                        contact.setIsSent(true);
                        notifyDataSetChanged();
                    }
                }
            }
        });

        String[] names = contact.getName().split(" ");
        if(names.length > 0) {
            holder.name.setText(names[0]);
        }
        holder.value.setText(contact.getValue());
        holder.status.setText(contact.getSendingStatus());

        try {
            pic.load(contact.getImage())
                    .error(R.drawable.default_contact_icon)
                    .placeholder(R.drawable.default_contact_icon)
                    .noFade()
                    .into(holder.image);
        }
        catch(Exception e){
            Logger.e("Error loading the contacts image", e);
        }

        holder.sentCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = ((CheckBox)v).isChecked();
                if(getOnContactActionListener() != null) {
                    getOnContactActionListener().onItemCheckedChange(contact, isChecked);
                }
            }
        });
            /*int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
            if(id != 0) {
                holder.sentCb.setButtonDrawable(id);
            }*/
        holder.sentCb.setChecked(contact.isSentOrSending());

        if(contact.isSent()) {
            holder.overlay.setBackgroundColor(Color.GRAY);
            holder.overlay.getBackground().setAlpha(155);
            holder.overlay.setVisibility(View.VISIBLE);
            holder.sentCb.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return true;
                }
            });
        }
        else {
            holder.overlay.setVisibility(View.GONE);
            holder.sentCb.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return false;
                }
            });
        }

        convertView.setTag(holder);

        return convertView;
    }

    private static class ViewHolder {
        View overlay;
        TextView name;
        TextView value;
        TextView status;
        CheckBox sentCb;
        ImageView image;
    }
}
