package com.chatwala.android.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.R;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.PutUserProfilePictureCommand;
import com.chatwala.android.util.MessageDataStore;
import com.squareup.picasso.Picasso;

import java.io.*;

/**
 * Created by matthewdavis on 1/10/14.
 */
public class UpdateProfilePicActivity extends BaseChatWalaActivity
{
    private String userId;
    private boolean isReview;
    private ImageView profilePicImage;
    private TextView buttonText, bottomPanelText, noImageText;

    private File newThumbImage = null, tempThumbImage = null;

    private static final int TAKE_PICTURE_REQUEST = 1000;
    private static final String IS_REVIEW = "IS_REVIEW";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile_pic);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        userId = AppPrefs.getInstance(UpdateProfilePicActivity.this).getUserId();
        isReview = getIntent().getBooleanExtra(IS_REVIEW, false);

        profilePicImage = (ImageView)findViewById(R.id.current_profile_pic);
        noImageText = (TextView)findViewById(R.id.no_profile_pic_text);
        File thumbImage = MessageDataStore.findUserImageInLocalStore(userId);
        if(thumbImage.exists())
        {
            Picasso.with(UpdateProfilePicActivity.this).load(thumbImage).skipMemoryCache().resize(350, 250).centerCrop().noFade().into(profilePicImage);
        }
        else
        {
            profilePicImage.setVisibility(View.GONE);
            noImageText.setVisibility(View.VISIBLE);
        }

        buttonText = (TextView)findViewById(R.id.take_profile_picture_button_text);
        bottomPanelText = (TextView)findViewById(R.id.change_profile_pic_bottom_panel_text);

        findViewById(R.id.take_profile_picture_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //tempThumbImage = MessageDataStore.makeTempUserFile(userId);
                //intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempThumbImage));
                startActivityForResult(intent, TAKE_PICTURE_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK)
        {
            profilePicImage.setVisibility(View.VISIBLE);
            noImageText.setVisibility(View.GONE);

            newThumbImage = MessageDataStore.makeUserFile(userId);
            if(newThumbImage.exists())
            {
                newThumbImage.delete();
            }

            try
            {
                FileOutputStream out = new FileOutputStream(newThumbImage);
                ((Bitmap)data.getExtras().get("data")).compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();

                DataProcessor.runProcess(new Runnable() {
                    @Override
                    public void run() {
                        BusHelper.submitCommandSync(UpdateProfilePicActivity.this, new PutUserProfilePictureCommand(newThumbImage.getPath(), true));
                    }
                });

                Toast.makeText(UpdateProfilePicActivity.this, "Profile updated!", Toast.LENGTH_LONG).show();
            }
            catch (FileNotFoundException e)
            {
                Toast.makeText(UpdateProfilePicActivity.this, "Problem updating profile, please try again", Toast.LENGTH_LONG).show();
            }
            catch (IOException e)
            {
                Toast.makeText(UpdateProfilePicActivity.this, "Problem updating profile, please try again", Toast.LENGTH_LONG).show();
            }

            UpdateProfilePicActivity.startMe(UpdateProfilePicActivity.this, isReview);
            finish();

        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
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

    @Override
    public void onBackPressed()
    {
        if(isReview)
        {
            AppPrefs.getInstance(UpdateProfilePicActivity.this).setImageReviewed();
            NewCameraActivity.startMe(UpdateProfilePicActivity.this);
            finish();
        }
        else
        {
            super.onBackPressed();
        }
    }

    public static void startMe(Context context, boolean isReview)
    {
        Intent intent = new Intent(context, UpdateProfilePicActivity.class);
        intent.putExtra(IS_REVIEW, isReview);
        context.startActivity(intent);
    }
}
