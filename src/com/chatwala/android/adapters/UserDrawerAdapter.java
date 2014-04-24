package com.chatwala.android.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.chatwala.android.R;
import com.chatwala.android.database.DrawerUser;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.TimestampUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Eliezer on 3/25/2014.
 */
public class UserDrawerAdapter extends BaseAdapter {
    private List<DrawerUser> users;
    private Picasso picLoader;
    private LayoutInflater inflater;
    private ColorDrawable statusPlaceholder = new ColorDrawable(Color.TRANSPARENT);

    public UserDrawerAdapter(Context context, List<DrawerUser> users, Picasso picLoader) {
        this.users = users;
        this.picLoader = picLoader;
        this.inflater = LayoutInflater.from(context);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public DrawerUser getItem(int i) {
        return users.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void useNewUsersList(List<DrawerUser> users) {
        if(users == null) {
            this.users = new ArrayList<DrawerUser>(0);
        }
        else {
            this.users = users;
        }
        notifyDataSetChanged();
    }

    public void removeBySenderId(String senderId) {
        Iterator<DrawerUser> it = users.iterator();
        while(it.hasNext()) {
            if(senderId.equals(it.next().getSenderId())) {
                it.remove();
                break;
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.row_drawer_thumb, parent, false);

            holder = new ViewHolder();
            holder.thumb = (ImageView) convertView.findViewById(R.id.drawer_item_thumb);
            holder.status = (ImageView) convertView.findViewById(R.id.drawer_item_status);
            holder.timestamp = (TextView) convertView.findViewById(R.id.drawer_item_time);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        final DrawerUser user = getItem(i);

        File thumbImage = MessageDataStore.findMessageUserThumbPathInLocalStore(user.getThumbnailUrl());
        if(thumbImage.exists()) {
            picLoader.load(thumbImage).noFade().into(holder.thumb);
        }
        else {
            picLoader.load(R.drawable.message_thumb).into(holder.thumb);
        }

        holder.timestamp.setText(TimestampUtils.formatMessageTimestamp(user.getTimestamp()));

        if(user.isUnread()) {
            picLoader.load(R.drawable.unread_icon).into(holder.status);
        }
        else {
            holder.status.setImageDrawable(statusPlaceholder);
        }

        convertView.setTag(holder);

        return convertView;
    }

    private static class ViewHolder {
        ImageView thumb;
        ImageView status;
        TextView timestamp;
    }
}
