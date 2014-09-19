package com.chatwala.android.camera;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class VideoMetadata implements Parcelable {
    private File video;
    private int height;
    private int width;
    private long duration;
    private long offset;
    private int rotation;

    public VideoMetadata(File video, int width, int height, long duration, int rotation) {
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
        duration = p.readLong();
        offset = p.readLong();
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

    public long getDuration() {
        return duration;
    }

    public int getRotation() {
        return rotation;
    }

    //////////////////////////////
    // Parcelable apis
    //////////////////////////////
    public static final Creator<VideoMetadata> CREATOR
            = new Creator<VideoMetadata>() {
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
        p.writeLong(duration);
        p.writeLong(offset);
        p.writeInt(rotation);
    }
    //////////////////////////////
    // end Parcelable apis
    //////////////////////////////
}
