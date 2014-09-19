package com.chatwala.android.util;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import com.chatwala.android.camera.VideoMetadata;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 7:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class VideoUtils {
    public static VideoMetadata parseVideoMetadata(File video) {
        MediaMetadataRetriever metaRetriever = null;
        try {
            metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(video.getPath());

            String widthStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String durationStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String rotationStr = null;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                rotationStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            }

            int width = 720;
            if(widthStr != null) {
                width = Integer.parseInt(widthStr);
            }

            int height = 480;
            if(heightStr != null) {
                height = Integer.parseInt(heightStr);
            }

            long duration = 0;
            if(durationStr != null) {
                duration = Long.parseLong(durationStr);
            }

            int rotation = 0;
            if(rotationStr != null) {
                rotation = Integer.parseInt(rotationStr);
            }

            return new VideoMetadata(video, width, height, duration, rotation);
        }
        catch(Exception e) {
            Logger.e("There was an error parsing the video metadata", e);
            return null;
        }
        finally {
            if(metaRetriever != null) {
                metaRetriever.release();
            }
        }
    }

    public static Bitmap createBitmapFromVideoFrame(File video, int positionMillis) {
        MediaMetadataRetriever metaRetriever = null;
        try {
            metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(video.getPath());

            return metaRetriever.getFrameAtTime(positionMillis * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        }
        catch(Throwable e) {
            Logger.e("Got an exception trying to create a bitmap from a video frame",e );
            return null;
        }
        finally {
            if(metaRetriever != null) {
                metaRetriever.release();
            }
        }
    }
}
