package com.chatwala.android.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.chatwala.android.R;
import com.chatwala.android.files.FileManager;
import com.chatwala.android.queue.jobs.UploadUserProfilePicJob;
import com.chatwala.android.users.UserManager;
import com.chatwala.android.util.BitmapUtils;
import com.chatwala.android.util.Logger;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.bitmap.Transform;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/16/2014
 * Time: 1:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProfilePicActivity extends BaseChatwalaActivity {
    private ImageView profilePicImage;
    private TextView noImageText;
    private File tempUserImage;
    private File userImage;

    private static final int TAKE_PICTURE_REQUEST = 1000;

    private Transform bitmapTransform = new Transform() {
        @Override
        public Bitmap transform(Bitmap bitmap) {
            try {
                return BitmapUtils.scaleBitmap(bitmap, 350, 250);
            }
            catch(Throwable e) {
                return bitmap;
            }
        }

        @Override
        public String key() {
            return Long.toString(userImage.lastModified());
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_pic_activity);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        profilePicImage = (ImageView)findViewById(R.id.current_profile_pic);
        noImageText = (TextView)findViewById(R.id.no_profile_pic_text);
        userImage = FileManager.getUserProfilePic();
        if(userImage.exists()) {
            Ion.with(ProfilePicActivity.this)
                    .load(userImage)
                    .withBitmap()
                    .transform(bitmapTransform)
                    .disableFadeIn()
                    .intoImageView(profilePicImage);
        }
        else {
            profilePicImage.setVisibility(View.GONE);
            noImageText.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.take_profile_picture_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                tempUserImage = new File(Environment.getExternalStorageDirectory(), "chatwala-" + UserManager.getUserId() + ".png");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempUserImage));
                startActivityForResult(intent, TAKE_PICTURE_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            profilePicImage.setVisibility(View.VISIBLE);
            noImageText.setVisibility(View.GONE);

            if(tempUserImage.exists()) {
                final ProgressDialog pd = ProgressDialog.show(this, "Saving Image", "Please Wait...", true);
                new AsyncTask<Void, Void, Boolean>() {

                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        try {
                            BitmapUtils.rotateBitmap(tempUserImage, userImage);
                            tempUserImage.delete();

                            UploadUserProfilePicJob.post();
                            return true;
                        }
                        catch(Exception e) {
                            Logger.e("Couldn't save new profile picture", e);
                            return false;
                        }
                    }

                    @Override
                    public void onPostExecute(Boolean result) {
                        if(result) {
                            Toast.makeText(ProfilePicActivity.this, "Profile updated!", Toast.LENGTH_LONG).show();
                        }
                        else {
                            Toast.makeText(ProfilePicActivity.this, "Problem updating profile, please try again", Toast.LENGTH_LONG).show();
                        }
                        if(pd != null && pd.isShowing()) {
                            pd.dismiss();
                        }
                        userImage.setLastModified(System.currentTimeMillis());
                        Ion.with(ProfilePicActivity.this)
                                .load(userImage)
                                .withBitmap()
                                .transform(bitmapTransform)
                                .disableFadeIn()
                                .intoImageView(profilePicImage);
                    }
                }.execute();
            }

        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
        finish();
    }
}

