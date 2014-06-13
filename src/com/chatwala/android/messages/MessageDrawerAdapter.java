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
import com.chatwala.android.queue.jobs.GetMessageImageJob;
import com.chatwala.android.queue.jobs.GetUserImageJob;
import com.chatwala.android.util.IonCacheControl;
import com.chatwala.android.util.TimestampUtils;
import com.koushikdutta.ion.Ion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 1:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageDrawerAdapter extends BaseAdapter {
    private Context context;
    private List<ChatwalaMessage> messages;
    private LayoutInflater inflater;
    private ColorDrawable statusPlaceholder = new ColorDrawable(Color.TRANSPARENT);

    public MessageDrawerAdapter(Context context, List<ChatwalaMessage> messages) {
        this.context = context;
        this.messages = messages;
        this.inflater = LayoutInflater.from(context);

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public ChatwalaMessage getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void useNewMessagesList(List<ChatwalaMessage> messages) {
        if(messages == null) {
            this.messages = new ArrayList<ChatwalaMessage>(0);
        }
        else {
            this.messages = messages;
        }
        notifyDataSetChanged();
    }

    public void remove(int i) {
        messages.remove(i);
        notifyDataSetChanged();
    }

    public void clearMessages() {
        messages = new ArrayList<ChatwalaMessage>(0);
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

        final ChatwalaMessage message = getItem(i);

        final File thumbImage = message.getLocalMessageThumb();
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
            GetMessageImageJob.post(message);
            GetUserImageJob.post(message.getSenderId(), message.getUserImageUrl());
            final File userThumb = message.getLocalUserThumb();
            if(userThumb.exists()) {
                Ion.with(context)
                        .load(userThumb)
                        .withBitmap()
                        .disableFadeIn()
                        .transform(new IonCacheControl(Long.toString(userThumb.lastModified())))
                        .error(R.drawable.message_thumb)
                        .intoImageView(holder.thumb);
            }
            else {
                holder.thumb.setImageResource(R.drawable.message_thumb);
            }
        }

        holder.timestamp.setText(TimestampUtils.formatMessageTimestamp(message.getTimestamp()));

        if(message.getMessageState() != null) {
            switch(message.getMessageState()) {
                case UNREAD:
                    holder.status.setImageResource(R.drawable.unread_icon);
                    break;
                case REPLIED:
                    holder.status.setImageResource(R.drawable.replied_icon);
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
