package com.chatwala.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Message;
import android.util.Log;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.R;
import org.apache.commons.io.IOUtils;

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


    public static void createThumbForMessage(Context context, byte[] imageBytes, String url) throws IOException {
        File tempFile = MessageDataStore.findMessageThumbTempPathInLocalStore(url);
        File thumbFile = MessageDataStore.findMessageThumbInLocalStore(url);
        //write to temp file
        InputStream is = new ByteArrayInputStream(imageBytes);
        FileOutputStream os = new FileOutputStream(tempFile);
        IOUtils.copy(is, os);
        os.close();
        is.close();

        Bitmap thumbBitmap = BitmapFactory.decodeFile(tempFile.getPath());

        if(thumbBitmap == null)
        {
            thumbBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.message_thumb);
        }
        else
        {
            Logger.d("Thumb width: " + thumbBitmap.getWidth() + "; thumb height: " + thumbBitmap.getHeight());

            thumbBitmap = rotateBitmap(tempFile, thumbBitmap);

            //don't need the temp file anymore
            tempFile.delete();

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
            FileOutputStream out = new FileOutputStream(thumbFile);
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


    public static void createThumbForUserImage(Context context, String userId)
    {
        File thumbImageFile = MessageDataStore.findUserImageThumbInLocalStore(userId);

        Bitmap thumbBitmap = BitmapFactory.decodeFile(MessageDataStore.findUserImageInLocalStore(userId).getPath());

        if(thumbBitmap == null)
        {
            thumbBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.message_thumb);
        }
        else
        {
            Logger.d("Thumb width: " + thumbBitmap.getWidth() + "; thumb height: " + thumbBitmap.getHeight());

            thumbBitmap = rotateBitmap(MessageDataStore.findUserImageInLocalStore(userId), thumbBitmap);

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

    public static Bitmap rotateBitmap(File regularImageFile, Bitmap bitmap)
    {
        try
        {
            //File regularImageFile = MessageDataStore.findUserImageInLocalStore(userId);
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
