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
import com.chatwala.android.database.DrawerMessage;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.TimestampUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eliezer on 3/25/2014.
 */
public class MessageDrawerAdapter extends BaseAdapter {
    private List<DrawerMessage> messages;
    private Picasso picLoader;
    private LayoutInflater inflater;
    private ColorDrawable statusPlaceholder = new ColorDrawable(Color.TRANSPARENT);

    public MessageDrawerAdapter(Context context, List<DrawerMessage> messages, Picasso picLoader) {
        this.messages = messages;
        this.picLoader = picLoader;
        this.inflater = LayoutInflater.from(context);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public DrawerMessage getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void useNewMessagesList(List<DrawerMessage> messages) {
        if(messages == null) {
            this.messages = new ArrayList<DrawerMessage>(0);
        }
        else {
            this.messages = messages;
        }
        notifyDataSetChanged();
    }

    public void clearMessages() {
        messages = new ArrayList<DrawerMessage>(0);
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

        final DrawerMessage message = getItem(i);

        File thumbImage = MessageDataStore.findMessageThumbInLocalStore(message.getThumbnailUrl());
        if(thumbImage.exists()) {
            picLoader.load(thumbImage).noFade().into(holder.thumb);
        }
        else {
            thumbImage = MessageDataStore.findMessageUserThumbPathInLocalStore(message.getUserThumbnailUrl());
            if(thumbImage.exists()) {
                picLoader.load(thumbImage).noFade().into(holder.thumb);
            }
            else {
                picLoader.load(R.drawable.message_thumb).into(holder.thumb);
            }
        }

        holder.timestamp.setText(TimestampUtils.formatMessageTimestamp(message.getTimestamp()));

        if(message.getMessageState() != null) {
            switch(message.getMessageState()) {
                case UNREAD:
                    picLoader.load(R.drawable.unread_icon).into(holder.status);
                    break;
                case REPLIED:
                    picLoader.load(R.drawable.replied_icon).into(holder.status);
                    break;
                default:
                    holder.status.setImageDrawable(statusPlaceholder);
            }
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
