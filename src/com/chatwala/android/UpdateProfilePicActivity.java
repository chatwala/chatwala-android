package com.chatwala.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.chatwala.android.util.MessageDataStore;
import com.squareup.picasso.Picasso;

import java.io.File;

/**
 * Created by matthewdavis on 1/10/14.
 */
public class UpdateProfilePicActivity extends BaseChatWalaActivity
{
    ImageView profilePicImage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile_pic);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        profilePicImage = (ImageView)findViewById(R.id.current_profile_pic);
        TextView noImageText = (TextView)findViewById(R.id.no_profile_pic_text);
        File thumbImage = MessageDataStore.findUserImageInLocalStore(AppPrefs.getInstance(UpdateProfilePicActivity.this).getUserId());
        if(thumbImage.exists())
        {
            Picasso.with(UpdateProfilePicActivity.this).load(thumbImage).resize(350, 250).centerCrop().noFade().into(profilePicImage);
        }
        else
        {
            profilePicImage.setVisibility(View.GONE);
            noImageText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static void startMe(Context context)
    {
        context.startActivity(new Intent(context, UpdateProfilePicActivity.class));
    }
}
