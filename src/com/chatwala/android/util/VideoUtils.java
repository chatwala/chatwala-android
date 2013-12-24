package com.chatwala.android.util;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/27/13
 * Time: 11:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class VideoUtils
{
    public static synchronized Bitmap createVideoFrame(String filePath, long ms)
    {
        CWLog.i(VideoUtils.class, "filePath: " + filePath + "/ms: " + ms);

        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try
        {
            retriever.setDataSource(filePath);
            int sourceRotation = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

            CWLog.b(VideoUtils.class, "sourceRotation: "+ sourceRotation);

            bitmap = retriever.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

            int rotation = sourceRotation - 270;
            if(rotation < 0)
                rotation += 360;


            /*if(rotation != 0)
            {
                int targetWidth = rotation == 180 ? bitmap.getWidth() : bitmap.getHeight();
                int targetHeight = rotation == 180 ? bitmap.getHeight() : bitmap.getWidth();
                Bitmap targetBitmap = Bitmap.createBitmap(targetWidth, targetHeight, bitmap.getConfig());
                Canvas canvas = new Canvas(targetBitmap);
                Matrix matrix = new Matrix();
                matrix.setRotate((float)rotation, bitmap.getWidth()/2,bitmap.getHeight()/2);
                canvas.drawBitmap(bitmap, matrix, new Paint());
                bitmap = targetBitmap;
            }*/
        }
        catch (IllegalArgumentException ex)
        {
            CWLog.i(VideoUtils.class, "", ex);
        }
        catch (RuntimeException ex)
        {
            CWLog.i(VideoUtils.class, "", ex);
        }
        finally
        {
            try
            {
                retriever.release();
            }
            catch (RuntimeException ex)
            {
                CWLog.i(VideoUtils.class, "", ex);
            }
        }

        return bitmap;
    }

    public static class VideoMetadata
    {
        public File videoFile;
        public int height;
        public int width;
        public int duration;
        public int rotation;
    }

    public static VideoMetadata findMetadata(File videoFile) throws IOException
    {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        FileInputStream inp = new FileInputStream(videoFile);

        metaRetriever.setDataSource(inp.getFD());
        String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

        String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String rotation = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);

        CWLog.i(VideoUtils.class, "rotation: "+ rotation);
        inp.close();
        VideoMetadata metadata = new VideoMetadata();

        metadata.videoFile = videoFile;
        metadata.height = Integer.parseInt(height);
        metadata.width = Integer.parseInt(width);
        metadata.duration = Integer.parseInt(duration);
        metadata.rotation = rotation == null ? 0 : Integer.parseInt(rotation);

        return metadata;
    }

}
