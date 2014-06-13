package com.chatwala.android.media;

import com.chatwala.android.camera.VideoMetadata;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/30/2014
 * Time: 5:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class CwVideoTrack implements CwTrack {
    private VideoMetadata metadata;
    private long playbackOffset;

    public CwVideoTrack(VideoMetadata metadata) {
        this(metadata, 0);
    }

    public CwVideoTrack(VideoMetadata metadata, long playbackOffset) {
        this.metadata = metadata;
        this.playbackOffset = playbackOffset;
    }

    private VideoMetadata getMetadata() {
        return metadata;
    }

    @Override
    public String getUri() {
        return metadata.getVideo().getAbsolutePath();
    }

    @Override
    public long getDuration() {
        return getMetadata().getDuration();
    }

    @Override
    public long getPlaybackOffset() {
        return playbackOffset;
    }

    @Override
    public long getSize() {
        return getMetadata().getVideo().length();
    }

    public int getWidth() {
        return getMetadata().getWidth();
    }

    public int getHeight() {
        return getMetadata().getHeight();
    }

    public File getVideo() {
        return getMetadata().getVideo();
    }

    public int getRotation() {
        return getMetadata().getRotation();
    }
}
