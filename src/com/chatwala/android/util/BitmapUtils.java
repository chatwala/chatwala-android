package com.chatwala.android.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 11:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class BitmapUtils {
    /**
     * Rotates a bitmap if needed.
     * @param bitmapFile
     * @return The file that holds the rotated bitmap
     */
    public static File rotateBitmap(File bitmapFile) {
        return rotateBitmap(bitmapFile, bitmapFile);
    }

    /**
     * Rotates a bitmap if needed.
     * @param fromFile
     * @param toFile
     * @return The file that holds the rotated bitmap
     */
    public static File rotateBitmap(File fromFile, File toFile) {
        try {
            Bitmap toRotate = BitmapFactory.decodeFile(fromFile.getAbsolutePath());
            Bitmap rotated = rotateBitmap(fromFile, toRotate);
            /*
            I don't think we need these here, but keep them ready to go
            toRotate = null;
            System.gc();*/
            FileUtils.writeBitmapToFile(rotated, toFile);
            return toFile;
        }
        catch(Exception e) {
            Logger.e("There was an error rotating a bitmap", e);
            return null;
        }
    }

    /**
     * Rotates a bitmap if needed.
     * @param bitmapsFile The File that the Bitmap was loaded from
     * @param originalBitmap The Bitmap to be rotated
     * @return The rotated bitmap
     */
    public static Bitmap rotateBitmap(File bitmapsFile, Bitmap originalBitmap) {
        try {
            ExifInterface exif = new ExifInterface(bitmapsFile.getPath());

            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            return rotateBitmap(orientation, originalBitmap);
        }
        catch(Exception e) {
            Logger.e("There was an error rotating a bitmap", e);
            return originalBitmap;
        }
    }

    /**
     * Rotates a bitmap if needed.
     * @param orientation The original bitmap's orientation
     * @param originalBitmap The Bitmap to be rotated
     * @return The rotated bitmap
     */
    public static Bitmap rotateBitmap(int orientation, Bitmap originalBitmap) {
        try {
            if(orientation == ExifInterface.ORIENTATION_NORMAL) {
                return originalBitmap;
            }
            else {
                Matrix matrix = new Matrix();
                switch(orientation) {
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        matrix.setScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.setRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        matrix.setRotate(180);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        matrix.setRotate(90);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.setRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        matrix.setRotate(-90);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.setRotate(-90);
                        break;
                    default:
                        return originalBitmap;
                }

                return Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(),
                        originalBitmap.getHeight(), matrix, true);
            }
        }
        catch(Throwable e) {
            Logger.e("There was an error rotating a bitmap", e);
            return originalBitmap;
        }
    }

    public static Bitmap scaleBitmap(Bitmap src, float targetWidth, float targetHeight) throws OutOfMemoryError {
        float idealRatio = targetHeight / targetWidth;
        int idealY = (int)(src.getWidth() * idealRatio);
        if(src.getHeight() < idealY) {
            idealY = src.getHeight();
        }
        int yMid = src.getHeight() / 2;
        return Bitmap.createBitmap(src, 0, yMid - (idealY / 2), src.getWidth(), idealY);
    }

    public static Bitmap loadScaledBitmap(File fromFile, int targetWidth, int targetHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fromFile.getAbsolutePath(), options);

        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight);
        return BitmapFactory.decodeFile(fromFile.getAbsolutePath(), options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int targetWidth, int targetHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > targetHeight || width > targetWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > targetHeight
                    && (halfWidth / inSampleSize) > targetWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
