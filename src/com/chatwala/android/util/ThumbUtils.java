package com.chatwala.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.chatwala.android.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by matthewdavis on 2/3/14.
 */
public class ThumbUtils
{
    public static void createThumbForUserImage(Context context, String userId)
    {
        File thumbImageFile = MessageDataStore.findUserImageThumbInLocalStore(userId);

        Bitmap thumbBitmap = BitmapFactory.decodeFile(MessageDataStore.findUserImageInLocalStore(userId).getPath());
        Log.d("######", "thumb width: " + thumbBitmap.getWidth() + "thumb height: " + thumbBitmap.getHeight());

        float thumbWidth = context.getResources().getDimension(R.dimen.thumb_width);
        float thumbHeight = context.getResources().getDimension(R.dimen.thumb_height);
        float idealRatio = thumbHeight/thumbWidth;
        int idealY = (int)((float)thumbBitmap.getWidth() * idealRatio);
        if(thumbBitmap.getHeight() < idealY)
        {
            idealY = thumbBitmap.getHeight();
        }

        Log.d("######", "ratio: " + idealRatio + " idealHeight: " + idealY);

        int yMid = thumbBitmap.getHeight()/2;
        thumbBitmap = thumbBitmap.createBitmap(thumbBitmap, 0, yMid - (idealY/2), thumbBitmap.getWidth(), idealY);

        try
        {
            FileOutputStream out = new FileOutputStream(thumbImageFile);
            thumbBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
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

        thumbBitmap.recycle();
        thumbBitmap = null;
    }
}
