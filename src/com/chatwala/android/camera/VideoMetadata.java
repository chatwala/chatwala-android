package com.chatwala.android.camera;

import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import com.chatwala.android.util.Logger;

import java.io.File;

public class VideoMetadata implements Parcelable {
    private File video;
    private int height;
    private int width;
    private int duration;
    private int rotation;

    public static VideoMetadata parseVideoMetadata(File video) {
        try {
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(video.getPath());

            String widthStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String durationStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String rotationStr = null;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            }

            int width = 720;
            if(widthStr != null) {
                width = Integer.parseInt(widthStr);
            }

            int height = 480;
            if(heightStr != null) {
                height = Integer.parseInt(heightStr);
            }

            int duration = 0;
            if(durationStr != null) {
                duration = Integer.parseInt(durationStr);
            }

            int rotation = 0;
            if(rotationStr != null) {
                rotation = Integer.parseInt(rotationStr);
            }

            return new VideoMetadata(video, height, width, duration, rotation);
        }
        catch(Exception e) {
            Logger.e("There was an error parsing the video metadata", e);
            return null;
        }
    }

    private VideoMetadata(File video, int width, int height, int duration, int rotation) {
        this.video = video;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.rotation = rotation;
    }

    public VideoMetadata(Parcel p) {
        video = new File(p.readString());
        width = p.readInt();
        height = p.readInt();
        duration = p.readInt();
        rotation = p.readInt();
    }

    public File getVideo() {
        return video;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDuration() {
        return duration;
    }

    public int getRotation() {
        return rotation;
    }

    //////////////////////////////
    // Parcelable apis
    //////////////////////////////
    public static final Parcelable.Creator<VideoMetadata> CREATOR
            = new Parcelable.Creator<VideoMetadata>() {
        public VideoMetadata createFromParcel(Parcel p) {
            return new VideoMetadata(p);
        }

        public VideoMetadata[] newArray(int size) {
            return new VideoMetadata[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeString(video.getPath());
        p.writeInt(width);
        p.writeInt(height);
        p.writeInt(duration);
        p.writeInt(rotation);
    }
    //////////////////////////////
    // end Parcelable apis
    //////////////////////////////
}
