package com.chatwala.android.messages;

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
import com.chatwala.android.queue.jobs.GetUserImageJob;
import com.chatwala.android.util.IonCacheControl;
import com.chatwala.android.util.TimestampUtils;
import com.koushikdutta.ion.Ion;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 12:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserDrawerAdapter extends BaseAdapter {
    private static final long CACHE_EXPIRATION_MILLIS = 1000 * 60 * 1; //one minute
    private Context context;
    private List<ChatwalaUser> users;
    private LayoutInflater inflater;
    private ColorDrawable statusPlaceholder = new ColorDrawable(Color.TRANSPARENT);

    public UserDrawerAdapter(Context context, List<ChatwalaUser> users) {
        this.context = context;
        this.users = users;
        this.inflater = LayoutInflater.from(context);

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public ChatwalaUser getItem(int i) {
        return users.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void useNewUsersList(List<ChatwalaUser> users) {
        if(users == null) {
            this.users = new ArrayList<ChatwalaUser>(0);
        }
        else {
            this.users = users;
        }
        notifyDataSetChanged();
    }

    public void removeBySenderId(String senderId) {
        Iterator<ChatwalaUser> it = users.iterator();
        while(it.hasNext()) {
            ChatwalaUser user = it.next();
            if(senderId != null && user != null && user.getSenderId() != null && senderId.equals(user.getSenderId())) {
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

        final ChatwalaUser user = getItem(i);

        final File thumbImage = user.getLocalThumb();
        if(!thumbImage.exists() || System.currentTimeMillis() - thumbImage.lastModified() > CACHE_EXPIRATION_MILLIS) {
            GetUserImageJob.post(user.getSenderId(), user.getThumbnailUrl());
        }
        if(thumbImage.exists()) {
            Ion.with(context)
                    .load(thumbImage)
                    .withBitmap()
                    .disableFadeIn()
                    .transform(new IonCacheControl(Long.toString(thumbImage.lastModified())))
                    .error(R.drawable.message_thumb)
                    .intoImageView(holder.thumb);
        }
        else {
            holder.thumb.setImageResource(R.drawable.message_thumb);
        }

        holder.timestamp.setText(TimestampUtils.formatMessageTimestamp(user.getTimestamp()));

        if(user.isUnread()) {
            holder.status.setImageResource(R.drawable.unread_icon);
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
