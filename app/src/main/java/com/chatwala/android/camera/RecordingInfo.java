package com.chatwala.android.camera;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
/*package*/ class RecordingInfo {
    private File recordingFile;
    private long recordingLength;

    private boolean manuallyStopped;

    public RecordingInfo(File recordingFile, long recordingLength, boolean manuallyStopped) {
        this.recordingFile = recordingFile;
        this.recordingLength = recordingLength;
        this.manuallyStopped = manuallyStopped;
    }

    public long getRecordingLength() {
        return recordingLength;
    }

    public File getRecordingFile() {
        return recordingFile;
    }

    public boolean wasManuallyStopped() {
        return manuallyStopped;
    }
}
