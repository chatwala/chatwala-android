package com.chatwala.android.camera;

import java.io.File;

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
