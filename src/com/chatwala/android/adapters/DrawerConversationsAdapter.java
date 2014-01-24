package com.chatwala.android.adapters;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.chatwala.android.activity.BaseNavigationDrawerActivity;
import com.chatwala.android.activity.NewCameraActivity;
import com.chatwala.android.R;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.TimestampUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
* Created by matthewdavis on 1/24/14.
*/
public class DrawerConversationsAdapter extends BaseDrawerAdapter
{
    BaseNavigationDrawerActivity activity;
    Picasso imageLoader;
    ArrayList<ChatwalaMessage> messageList;

    public DrawerConversationsAdapter(BaseNavigationDrawerActivity activity, Picasso imageLoader, List<ChatwalaMessage> messageList)
    {
        this.activity = activity;
        this.imageLoader = imageLoader;
        this.messageList = new ArrayList<ChatwalaMessage>(messageList);
        Collections.sort(this.messageList, new Comparator<ChatwalaMessage>() {
            @Override
            public int compare(ChatwalaMessage lhs, ChatwalaMessage rhs) {
                return lhs.getSortId() - rhs.getSortId();
            }
        });
    }

    @Override
    public int getCount()
    {
        return messageList.size();
    }

    @Override
    public Object getItem(int position)
    {
        return messageList.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if(convertView == null)
        {
            convertView = activity.getLayoutInflater().inflate(R.layout.row_drawer_thumb, parent, false);
            convertView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    NewCameraActivity.startMeWithId(activity, (String) v.getTag());
                    activity.finish();
                }
            });
        }

        final ChatwalaMessage message = (ChatwalaMessage)getItem(position);

        ImageView thumbView = (ImageView) convertView.findViewById(R.id.thumb_view);
        File thumbImage = MessageDataStore.findUserImageInLocalStore(message.getSenderId());
        imageLoader.load(thumbImage).resize(150,70).centerCrop().noFade().into(thumbView);

        if(message.getTimestamp() != null)
        {
            ((TextView)convertView.findViewById(R.id.time_since_text)).setText(TimestampUtils.formatMessageTimestamp(message.getTimestamp()));
        }

        ImageView stateView = (ImageView)convertView.findViewById(R.id.status_image);

        if(message.getMessageState() != null)
        {
            switch(message.getMessageState())
            {
                case UNREAD:
                    stateView.setImageResource(R.drawable.unread_icon);
                    break;
                case REPLIED:
                    stateView.setImageResource(R.drawable.replied_icon);
                    break;
                default:
                    stateView.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }

        convertView.setTag(message.getMessageId());

        return convertView;
    }
}
