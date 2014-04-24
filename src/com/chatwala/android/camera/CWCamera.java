package com.chatwala.android.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.MessageDataStore;

import java.io.File;
import java.util.List;

/**
 * Created by Eliezer on 4/18/2014.
 */
public class CWCamera {
    private Camera camera;
    private int cameraId;
    private CameraType cameraType = CameraType.FRONT;
    private CameraState cameraState = CameraState.CLOSED;
    private Camera.Size bestPreviewSize;
    private MediaRecorder recorder;

    private int displayRotation;

    private File recordingFile;

    private enum CameraState {
        CLOSED, READY, PREVIEW, RECORDING, ERROR
    }

    private enum ErrorCause {
        CAMERA_ERROR, NO_CAMERA, PREVIEW, UNLOCK_CAMERA, RECORDER_PARAMS, RECORDER_START, RECORDER_STOP
    }

    private enum CameraType {
        FRONT, BACK
    }

    private CWCamera() {}

    private static class Singleton {
        public static final CWCamera instance = new CWCamera();
    }

    /*package*/ static CWCamera getInstance() {
        return Singleton.instance;
    }

    private void error(ErrorCause cause) {
        Logger.e("Official CWCamera error - " + cause.toString());
        release();
        cameraState = CameraState.ERROR;
    }

    public void init(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        displayRotation = display.getRotation();
    }

    public boolean init(int width, int height) {
        Logger.i("Camera initting");

        cameraType = CameraType.FRONT;
        if(!openCamera(width, height)) {
            return false;
        }

        cameraState = CameraState.READY;

        if(camera != null) {
            camera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int i, Camera camera) {
                    Logger.e("Got an error from the camera (error = " + i + ")");
                    error(ErrorCause.CAMERA_ERROR);
                }
            });
        }

        Logger.i("Camera initted");
        return true;
    }

    public boolean toggleCamera(int width, int height) {
        Logger.i("Camera toggling");

        if(cameraState != CameraState.PREVIEW) {
            Logger.w("Can't toggle cameras in this state (" + cameraState.toString() + ")");
            return false;
        }

        if(cameraType == CameraType.FRONT) {
            cameraType = CameraType.BACK;
        }
        else if(cameraType == CameraType.BACK) {
            cameraType = CameraType.FRONT;
        }

        release();

        if(!openCamera(width, height)) {
            return false;
        }

        cameraState = CameraState.READY;

        if(camera != null) {
            camera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int i, Camera camera) {
                    Logger.e("Got an error from the camera (error = " + i + ")");
                    error(ErrorCause.CAMERA_ERROR);
                }
            });
        }

        Logger.i("Camera toggled");
        return true;
    }

    public boolean canRecord() {
        return cameraState == CameraState.PREVIEW && !hasError();
    }

    public boolean hasError() {
        return cameraState == CameraState.ERROR;
    }

    public boolean isShowingPreview() {
        return cameraState == CameraState.PREVIEW;
    }

    public boolean isRecording() {
        return cameraState == CameraState.RECORDING;
    }

    public File getRecordingFile() {
        return recordingFile;
    }

    public Camera.Size getPreviewSize() {
        return bestPreviewSize;
    }

    public boolean attachToPreview(SurfaceTexture surface) {
        if(!hasError() && camera != null) {
            try {
                if(isShowingPreview()) {
                    camera.stopPreview();
                }

                camera.setPreviewTexture(surface);
                camera.startPreview();

                cameraState = CameraState.PREVIEW;
                Logger.i("Started preview");
                return true;
            }
            catch(Exception e) {
                Logger.e("Couldn't attach the camera to the preview", e);
                error(ErrorCause.PREVIEW);
                return false;
            }
        }
        else {
            return false;
        }
    }

    public boolean stopPreview() {
        if(!hasError() && camera != null && cameraState == CameraState.PREVIEW) {
            camera.stopPreview();
            cameraState = CameraState.READY;
            Logger.i("Stopped preview");
            return true;
        }
        else {
            return false;
        }
    }

    public boolean startRecording() {
        if(!hasError()) {
            try {
                camera.unlock();
                recorder.start();
                cameraState = CameraState.RECORDING;
                Logger.i("Start recording");
                return true;
            }
            catch(Exception e) {
                try {
                    camera.reconnect();
                }
                catch(Exception e2) {
                    error(ErrorCause.RECORDER_START);
                }
                return false;
            }
        }
        else {
            return false;
        }
    }

    public boolean stopRecording() {
        if(!hasError()) {
            try {
                recorder.stop();
                recorder.reset();
                cameraState = CameraState.READY;
                Logger.i("Stopped recording");
                return true;
            }
            catch(Exception e) {
                if(recordingFile != null && recordingFile.exists()) {
                    recorder.reset();
                    Logger.i("Stopped recording");
                    return true;
                }
                else {
                    error(ErrorCause.RECORDER_STOP);
                    return false;
                }
            }
        }
        else {
            return false;
        }
    }

    private boolean openCamera(int width, int height) {
        releaseCameras();
        int cameraCount = Camera.getNumberOfCameras();
        try {
            if(cameraType == CameraType.FRONT) {
                if(cameraCount > 1) {
                    camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                    cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }
                else if(cameraCount == 1) {
                    camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                    cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                else {
                    error(ErrorCause.NO_CAMERA);
                    return false;
                }
            }
            else if(cameraType == CameraType.BACK) {
                if(cameraCount >= 1) {
                    camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                    cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                else {
                    error(ErrorCause.NO_CAMERA);
                    return false;
                }
            }

            if(camera == null) {
                error(ErrorCause.NO_CAMERA);
                return false;
            }

            setupParameters(width, height);

            Logger.i("Opened the camera");
            return true;
        }
        catch(Exception e) {
            Logger.e("Couldn't open the camera", e);
            return false;
        }
    }

    private void setupParameters(int width, int height) {
        setCameraParameters(width, height);
        initMediaRecorder(width, height);
    }

    private void setCameraParameters(int width, int height) {
        try {
            if(camera != null) {
                Camera.Parameters params = camera.getParameters();
                params.setRecordingHint(true);
                int[] bestFrameFrate = getBestCameraFrameRate(params.getSupportedPreviewFpsRange());
                params.setPreviewFpsRange(bestFrameFrate[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                                          bestFrameFrate[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
                bestPreviewSize = getPreviewSize(width, height, params);
                if(bestPreviewSize != null) {
                    params.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
                }

                if (displayRotation == Surface.ROTATION_0) {
                    camera.setDisplayOrientation(90);
                }
                if (displayRotation == Surface.ROTATION_270) {
                    camera.setDisplayOrientation(180);
                }

                camera.setParameters(params);
            }
        }
        catch(Exception e) {
            Logger.w("Couldn't set the camera parameters", e);
        }
    }

    private int[] getBestCameraFrameRate(List<int[]> rateRanges) {
        return rateRanges.get(rateRanges.size() - 1);
    }

    private Camera.Size getPreviewSize(int width, int height, Camera.Parameters params) {
        if(camera != null) {
            if(cameraState == CameraState.PREVIEW) {
                camera.stopPreview();
            }

            return getBestSize(width, height, params.getSupportedPreviewSizes());
        }
        else {
            return null;
        }
    }

    private boolean initMediaRecorder(int width, int height) {
        if(recorder != null) {
            releaseRecorder();
        }

        recorder = new MediaRecorder();
        if(!setRecorderParams(width, height)) {
            releaseRecorder();
            try {
                camera.reconnect();
            } catch(Exception e) {
                Logger.e("Couldn't reconnect to the camera", e);
            }
            error(ErrorCause.RECORDER_PARAMS);
            return false;
        }
        Logger.i("Initted media recorder");
        return true;
    }

    private boolean setRecorderParams(int width, int height) {
        if(camera != null && recorder != null) {
            try {
                Camera.Parameters params = camera.getParameters();

                recorder.setCamera(camera);

                //must be before setOutputFormat
                recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

                //everything under here until prepare must be after setVideoSource and setOutputFormat
                int[] frameRateRange = new int[2];
                params.getPreviewFpsRange(frameRateRange);
                int frameRate = frameRateRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
                frameRate = (int) Math.floor(((double)frameRate / 1000));
                recorder.setVideoFrameRate(frameRate);

                List<Camera.Size> sizes = params.getSupportedVideoSizes();
                if(sizes == null) {
                    sizes = params.getSupportedPreviewSizes();
                }
                Camera.Size bestSize = getBestSize(width, height, sizes);

                if(bestSize != null) {
                    recorder.setVideoSize(bestSize.width, bestSize.height);
                }

                recorder.setVideoEncodingBitRate(CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH).videoBitRate);

                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

                int mrRotate = 0;
                if (displayRotation == Surface.ROTATION_0) {
                    mrRotate = 270;
                }
                else if (displayRotation == Surface.ROTATION_270) {
                    mrRotate = 180;
                }

                //when we record from the back with the above rotations
                //the video ends up getting flipped upside down
                //this flips it rightside up
                if(cameraType == CameraType.BACK && mrRotate != 0) {
                    mrRotate -= 180;
                }

                if (mrRotate != 0) {
                    recorder.setOrientationHint(mrRotate);
                }

                if(recordingFile != null && recordingFile.exists()) {
                    recordingFile.delete();
                }
                recordingFile = MessageDataStore.makeTempVideoFile();
                recorder.setOutputFile(recordingFile.getPath());

                recorder.prepare();

                Logger.i("Set the media recorder params");
                return true;
            }
            catch(Exception e) {
                Logger.e("Couldn't set the recorder params", e);
                return false;
            }
        }
        else {
            return false;
        }
    }

    private Camera.Size getBestSize(int width, int height, List<Camera.Size> sizes) {
        Camera.Size bestSize = null;
        for (Camera.Size size : sizes) {
            if (size.width <= width && size.height <= height) {
                if (bestSize == null) {
                    bestSize = size;
                }
                else {
                    int resultArea = bestSize.width * bestSize.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        bestSize = size;
                    }
                }
            }
        }
        return bestSize;
    }

    public void release() {
        releaseResources();

        if(recordingFile != null && recordingFile.exists()) {
            recordingFile.delete();
        }

        Logger.i("CWCamera released");
    }

    private void releaseResources() {
        releaseRecorder();
        releaseCameras();

        cameraState = CameraState.CLOSED;
    }

    private void releaseCameras() {
        if(camera != null) {
            if(isShowingPreview()) {
                camera.stopPreview();
            }
            camera.release();
            camera = null;
        }

        Logger.i("Released camera");
    }

    private void releaseRecorder() {
        if(recorder != null) {
            if(isRecording()) {
                stopRecording();
            }

            recorder.reset();
            recorder.release();
            recorder = null;
        }
        Logger.i("Released media recorder");
    }
}
