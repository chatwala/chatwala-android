package com.chatwala.android.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.chatwala.android.activity.BaseNavigationDrawerActivity;
import com.chatwala.android.activity.NewCameraActivity;
import com.chatwala.android.R;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DrawerMessageInfo;
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
    public DrawerConversationsAdapter(BaseNavigationDrawerActivity activity, Picasso imageLoader, List<DrawerMessageInfo> messageList)
    {
        super(activity, imageLoader, messageList);
    }

    @Override
    protected Comparator<DrawerMessageInfo> getMessageComparator()
    {
        return new Comparator<DrawerMessageInfo>()
        {
            @Override
            public int compare(DrawerMessageInfo lhs, DrawerMessageInfo rhs) {
                return lhs.getSortId() - rhs.getSortId();
            }
        };
    }
}
