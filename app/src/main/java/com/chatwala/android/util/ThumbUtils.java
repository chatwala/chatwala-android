package com.chatwala.android.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 11:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThumbUtils {
    public static File createThumbFromImage(File fromFile, File toFile, float thumbWidth, float thumbHeight) {
        BufferedOutputStream bos = null;
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(fromFile.getAbsolutePath());
            bitmap = createThumb(bitmap, thumbWidth, thumbHeight);
            bos = new BufferedOutputStream(new FileOutputStream(toFile));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            return toFile;
        }
        catch(Throwable e) {
            Logger.e("Got an error while creating a thumb", e);
            return null;
        }
        finally {
            if(bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch(Exception ignore) {}
            }
        }
    }

    public static Bitmap createThumb(Bitmap bitmap, float thumbWidth, float thumbHeight) throws Throwable {
        return BitmapUtils.scaleBitmap(bitmap, thumbWidth, thumbHeight);
    }
}
