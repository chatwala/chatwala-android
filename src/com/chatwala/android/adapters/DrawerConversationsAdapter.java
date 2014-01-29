package com.chatwala.android.adapters;

import com.chatwala.android.activity.BaseNavigationDrawerActivity;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DrawerMessageWrapper;
import com.squareup.picasso.Picasso;

import java.util.Comparator;
import java.util.List;

/**
* Created by matthewdavis on 1/24/14.
*/
public class DrawerConversationsAdapter extends BaseDrawerAdapter
{
    public DrawerConversationsAdapter(BaseNavigationDrawerActivity activity, Picasso imageLoader, List<ChatwalaMessage> messageList)
    {
        super(activity, imageLoader, messageList);
    }

    @Override
    protected Comparator<DrawerMessageWrapper> getMessageComparator()
    {
        return new Comparator<DrawerMessageWrapper>()
        {
            @Override
            public int compare(DrawerMessageWrapper lhs, DrawerMessageWrapper rhs) {
                return lhs.getSortId() - rhs.getSortId();
            }
        };
    }
}
