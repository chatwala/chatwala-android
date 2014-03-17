package com.chatwala.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.R;

import java.io.*;

/**
 * Created by matthewdavis on 2/3/14.
 */
public class ThumbUtils
{

    public static File createThumbFromFirstFrame(Context context, String videoFilePath) {

        Bitmap frame = VideoUtils.createVideoFrame(videoFilePath, 1);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        frame.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] toReturn = stream.toByteArray();

        InputStream is = new ByteArrayInputStream(stream.toByteArray());
        File file = MessageDataStore.findUserImageInLocalStore(AppPrefs.getInstance(context).getUserId());
        try
        {
            FileOutputStream os = new FileOutputStream(file);


            final byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }

            os.close();
            is.close();
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return file;
    }

    public static void createThumbForUserImage(Context context, String userId)
    {
        File thumbImageFile = MessageDataStore.findUserImageThumbInLocalStore(userId);

        Bitmap thumbBitmap = BitmapFactory.decodeFile(MessageDataStore.findUserImageInLocalStore(userId).getPath());

        if(thumbBitmap == null)
        {
            thumbBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.appicon);
        }
        else
        {
            Logger.d("Thumb width: " + thumbBitmap.getWidth() + "; thumb height: " + thumbBitmap.getHeight());

            thumbBitmap = rotateBitmap(userId, thumbBitmap);

            float thumbWidth = context.getResources().getDimension(R.dimen.thumb_width);
            float thumbHeight = context.getResources().getDimension(R.dimen.thumb_height);
            float idealRatio = thumbHeight/thumbWidth;
            int idealY = (int)((float)thumbBitmap.getWidth() * idealRatio);
            if(thumbBitmap.getHeight() < idealY)
            {
                idealY = thumbBitmap.getHeight();
            }

            Logger.d("Thumb ratio: " + idealRatio + "; thumb ideal height: " + idealY);

            int yMid = thumbBitmap.getHeight()/2;
            thumbBitmap = thumbBitmap.createBitmap(thumbBitmap, 0, yMid - (idealY/2), thumbBitmap.getWidth(), idealY);
        }

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

    public static Bitmap rotateBitmap(String userId, Bitmap bitmap)
    {
        try
        {
            File regularImageFile = MessageDataStore.findUserImageInLocalStore(userId);
            ExifInterface exifInterface = new ExifInterface(regularImageFile.getPath());
            int orientation = Integer.parseInt(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION));

            if (orientation == 1)
            {
                return bitmap;
            }

            Matrix matrix = new Matrix();
            switch (orientation) {
                case 2:
                    matrix.setScale(-1, 1);
                    break;
                case 3:
                    matrix.setRotate(180);
                    break;
                case 4:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case 5:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case 6:
                    matrix.setRotate(90);
                    break;
                case 7:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    break;
                case 8:
                    matrix.setRotate(-90);
                    break;
                default:
                    return bitmap;
            }

            try {
                Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return oriented;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                return bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }
}
