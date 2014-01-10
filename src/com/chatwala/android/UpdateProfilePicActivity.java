package com.chatwala.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import co.touchlab.android.superbus.BusHelper;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.superbus.PutUserProfilePictureCommand;
import com.chatwala.android.util.MessageDataStore;
import com.squareup.picasso.Picasso;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * Created by matthewdavis on 1/10/14.
 */
public class UpdateProfilePicActivity extends BaseChatWalaActivity
{
    private String userId;
    private ImageView profilePicImage;
    private TextView buttonText, bottomPanelText;

    File newThumbImage = null;

    private static final int TAKE_PICTURE_REQUEST = 1000;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile_pic);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        userId = AppPrefs.getInstance(UpdateProfilePicActivity.this).getUserId();

        profilePicImage = (ImageView)findViewById(R.id.current_profile_pic);
        TextView noImageText = (TextView)findViewById(R.id.no_profile_pic_text);
        File thumbImage = MessageDataStore.findUserImageInLocalStore(userId);
        if(thumbImage.exists())
        {
            Picasso.with(UpdateProfilePicActivity.this).load(thumbImage).resize(350, 250).centerCrop().noFade().into(profilePicImage);
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
                if(newThumbImage == null)
                {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, TAKE_PICTURE_REQUEST);
                }
                else
                {
                    new AsyncTask<Void, Void, Boolean>()
                    {
                        @Override
                        protected Boolean doInBackground(Void... params)
                        {
                            try
                            {
                                final File userProfileFile = MessageDataStore.makeUserFile(userId);
                                FileOutputStream out = new FileOutputStream(userProfileFile);
                                FileInputStream in = new FileInputStream(newThumbImage);

                                IOUtils.copy(in, out);

                                newThumbImage.delete();

                                DataProcessor.runProcess(new Runnable() {
                                    @Override
                                    public void run() {
                                        BusHelper.submitCommandSync(UpdateProfilePicActivity.this, new PutUserProfilePictureCommand(userProfileFile.getPath(), true));
                                    }
                                });
                            }
                            catch (FileNotFoundException e)
                            {
                                return false;
                            }
                            catch (IOException e)
                            {
                                return false;
                            }

                            return true;
                        }

                        @Override
                        protected void onPostExecute(Boolean aBoolean)
                        {
                            if(aBoolean)
                            {
                                Toast.makeText(UpdateProfilePicActivity.this, "Profile updated!", Toast.LENGTH_LONG).show();
                            }
                            else
                            {
                                Toast.makeText(UpdateProfilePicActivity.this, "Problem updating profile, please try again later", Toast.LENGTH_LONG).show();
                            }

                            finish();
                        }
                    }.execute();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK)
        {
            try
            {
                FileOutputStream out = new FileOutputStream(MessageDataStore.makeTempUserFile(userId));
                ((Bitmap)data.getExtras().get("data")).compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
            }
            catch (FileNotFoundException e)
            {
                throw new RuntimeException(e);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            newThumbImage = MessageDataStore.makeTempUserFile(userId);
            Picasso.with(UpdateProfilePicActivity.this).load(newThumbImage).resize(350, 250).centerCrop().noFade().into(profilePicImage);

            buttonText.setText(R.string.save_profile_pic);
            bottomPanelText.setText(R.string.save_profile_pic_bottom_panel);
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

    public static void startMe(Context context)
    {
        context.startActivity(new Intent(context, UpdateProfilePicActivity.class));
    }
}
