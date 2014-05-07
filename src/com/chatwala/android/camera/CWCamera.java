package com.chatwala.android.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.util.Logger;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
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

    private int containerWidth;
    private int containerHeight;
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
        Logger.d("CWCamera display rotation is " + displayRotation);
    }

    public boolean init(int width, int height) {
        Logger.d("Camera initting");

        containerWidth = width;
        containerHeight = height;

        cameraType = CameraType.FRONT;
        if(!openCamera()) {
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

        Logger.d("Camera initted");
        return true;
    }

    public boolean toggleCamera() {
        Logger.d("Camera toggling");

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

        if(!openCamera()) {
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

        Logger.d("Camera toggled");
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

    public Camera.Size getPreviewSize() {
        return bestPreviewSize;
    }

    public boolean attachToPreview(SurfaceTexture surface) {
        if(!hasError() && camera != null) {
            try {
                if(isShowingPreview()) {
                    Logger.d("Can't start preview until we stop the previous one");
                    camera.stopPreview();
                }

                camera.setPreviewTexture(surface);
                camera.startPreview();

                cameraState = CameraState.PREVIEW;
                Logger.d("Started preview");
                return true;
            }
            catch(Exception e) {
                Logger.e("Couldn't attach the camera to the preview", e);
                error(ErrorCause.PREVIEW);
                return false;
            }
        }
        else {
            Logger.e("Couldn't attach the camera to the preview because we're in an error state");
            return false;
        }
    }

    public boolean stopPreview() {
        if(!hasError() && camera != null && cameraState == CameraState.PREVIEW) {
            camera.stopPreview();
            cameraState = CameraState.READY;
            Logger.d("Stopped preview");
            return true;
        }
        else {
            Logger.e("Couldn't stop preview because we're in an error state");
            return false;
        }
    }

    private long recordingLength;
    public boolean startRecording(int maxRecordingMillis, MediaRecorder.OnInfoListener infoListener) {
        if(!hasError()) {
            try {
                recorder.setMaxDuration(maxRecordingMillis);
                recorder.setOnInfoListener(infoListener);
                recorder.prepare();
                camera.unlock();
                recorder.start();
                recordingLength = System.currentTimeMillis();
                cameraState = CameraState.RECORDING;
                Logger.d("Start recording");
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
            Logger.e("Couldn't start recording because we're in an error state");
            return false;
        }
    }

    public RecordingInfo stopRecording(boolean actuallyStop) {
        File recordedFile = recordingFile;
        if(!hasError()) {
            try {
                if(actuallyStop) {
                    //we need this internal try...catch because we use maxDuration which calls stop in the background
                    try {
                        recorder.stop();
                        recordingLength = System.currentTimeMillis() - recordingLength;
                        if(recordingLength > 10000) {
                            recordingLength = 10000;
                        }
                    }
                    catch(Exception e) {
                        recordingLength = 10000;
                    }
                }
                else {
                    recordingLength = 10000;
                }
                cameraState = CameraState.PREVIEW;
                initMediaRecorder(false);
                Logger.d("Stopped recording");
                return new RecordingInfo(recordedFile, recordingLength, actuallyStop);
            }
            catch(Exception e) {
                if(recordingFile != null && recordingFile.exists()) {
                    initMediaRecorder(true);
                    Logger.d("Stopped recording");
                    return new RecordingInfo(recordedFile, recordingLength, actuallyStop);
                }
                else {
                    error(ErrorCause.RECORDER_STOP);
                    return null;
                }
            }
        }
        else {
            Logger.e("Couldn't stop recording because we're in an error state");
            return null;
        }
    }

    private boolean openCamera() {
        releaseCamera();
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

            setupParameters();

            Logger.d("Opened the camera");
            return true;
        }
        catch(Exception e) {
            Logger.e("Couldn't open the camera", e);
            return false;
        }
    }

    private void setupParameters() {
        setCameraParameters();
        initMediaRecorder(true);
    }

    private void setCameraParameters() {
        try {
            if(camera != null) {
                Camera.Parameters params = camera.getParameters();
                //params.set("cam_mode", 1 );
                //params.setRecordingHint(true);
                int[] bestFrameFrate = getBestCameraFrameRate(params.getSupportedPreviewFpsRange());
                params.setPreviewFpsRange(bestFrameFrate[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                                          bestFrameFrate[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
                Logger.d("Camera preview fps range is " + bestFrameFrate[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
                        + "-" + bestFrameFrate[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
                bestPreviewSize = getPreviewSize(containerWidth, containerHeight, params);
                if(bestPreviewSize != null) {
                    Logger.d("Camera preview size is " + bestPreviewSize.width + "x" + bestPreviewSize.height);
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

            return getBestSize(width, height, params.getSupportedPreviewSizes(), 0.5);
        }
        else {
            return null;
        }
    }

    private boolean initMediaRecorder(boolean releaseRecorder) {
        if(recorder != null) {
            if(releaseRecorder) {
                releaseRecorder();
                recorder = new MediaRecorder();
            }
            else {
                recorder.reset();
            }
        }
        else {
            recorder = new MediaRecorder();
        }

        if(!setRecorderParams(containerWidth, containerHeight)) {
            releaseRecorder();
            try {
                camera.reconnect();
            } catch(Exception e) {
                Logger.e("Couldn't reconnect to the camera", e);
            }
            error(ErrorCause.RECORDER_PARAMS);
            return false;
        }
        Logger.d("Initted media recorder");
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
                Logger.d("MediaRecorder framerate is " + frameRate);
                recorder.setVideoFrameRate(frameRate);

                List<Camera.Size> sizes = params.getSupportedVideoSizes();
                if(sizes == null) {
                    sizes = params.getSupportedPreviewSizes();
                }
                Camera.Size bestSize = getBestSize(width, height, sizes, 0.5);

                if(bestSize != null) {
                    Logger.d("MediaRecorder video size is " + bestSize.width + "x" + bestSize.height);
                    recorder.setVideoSize(bestSize.width, bestSize.height);
                }

                int bitrate = 600000; //CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH).videoBitRate;
                //TODO this will eventually use DeviceManager so we can use resources to get the bitrate
                //if (DeviceUtils.isDeviceS4()) {
                    bitrate = 1600000;
                //}
                Logger.d("MediaRecorder bitrate is " + bitrate);
                recorder.setVideoEncodingBitRate(bitrate);

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
                    Logger.d("MediaRecorder rotation is " + mrRotate);
                    recorder.setOrientationHint(mrRotate);
                }

                recordingFile = MessageManager.getInstance().getNewRecordingFile();
                recorder.setOutputFile(recordingFile.getPath());

                Logger.d("Set the media recorder params");
                return true;
            }
            catch(Exception e) {
                Logger.e("Couldn't set the recorder params", e);
                return false;
            }
        }
        else {
            Logger.e("Couldn't set media recorder params because the camera, the recorder, or both are null");
            return false;
        }
    }

    private Camera.Size getBestSize(int width, int height, List<Camera.Size> sizes, double aspectTolerance) {
        //COMMONSWARE
        /*double targetRatio = (double) width / height;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        if (displayRotation == 90 || displayRotation == 270) {
            targetRatio = (double) height / width;
        }

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;

            if (Math.abs(ratio - targetRatio) <= aspectTolerance) {
                if (Math.abs(size.height - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - height);
                }
            }
        }

        // Cannot find the one match the aspect ratio, ignore
        // the requirement
        if (optimalSize == null) {
            minDiff=Double.MAX_VALUE;

            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - height);
                }
            }
        }

        return optimalSize;*/

        //TOUCHLAB
        /*Camera.Size best = sizes.get(0);

        //Get biggest
        for (Camera.Size supportedVideoSize : sizes) {
            if(supportedVideoSize.width > best.width) {
                best = supportedVideoSize;
            }
        }

        for (int i = 1; i < sizes.size(); i++) {
            Camera.Size size = sizes.get(i);
            if (size.width >= height && size.width < best.width) {
                best = size;
            }
        }

        return best;*/

        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                int leftProduct = lhs.height * lhs.width;
                int rightProduct = rhs.height * rhs.width;
                if(leftProduct > rightProduct) {
                    return -1;
                }
                else if(leftProduct < rightProduct) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        });

        int screenProduct = height * width;
        for(Camera.Size size : sizes) {
            int sizeProduct = size.height * size.width;
            if(sizeProduct < screenProduct) {
                return size;
            }
        }

        return null;
    }

    public void release() {
        release(true);
    }

    public void release(boolean shouldDeleteRecordingFile) {
        releaseResources();

        if(shouldDeleteRecordingFile && recordingFile != null && recordingFile.exists()) {
            Logger.d("Deleting temp recording file");
            recordingFile.delete();
        }

        Logger.d("CWCamera released");
    }

    private void releaseResources() {
        releaseRecorder();
        releaseCamera();

        cameraState = CameraState.CLOSED;
    }

    private void releaseCamera() {
        if(camera != null) {
            if(isShowingPreview()) {
                Logger.d("Need to stop preview before we release the camera");
                camera.stopPreview();
            }
            camera.release();
            camera = null;
            Logger.d("Released camera");
        }
    }

    private void releaseRecorder() {
        if(recorder != null) {
            if(isRecording()) {
                Logger.d("Need to stop recording before we release the media recorder");
                stopRecording(true);
            }

            recorder.reset();
            recorder.release();
            recorder = null;
            Logger.d("Released media recorder");
        }
    }
}
