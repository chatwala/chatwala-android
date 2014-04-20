package com.chatwala.android.camera;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.MessageDataStore;

import java.io.File;
import java.util.List;

/**
 * Created by Eliezer on 4/18/2014.
 */
public class CWCamera {
    private boolean isInitted = false;
    private Camera frontCamera;
    private Camera backCamera;
    private int frontCameraId;
    private int backCameraId;
    private CameraType cameraType = CameraType.FRONT;
    private CameraState cameraState = CameraState.CLOSED;
    private MediaRecorder recorder;

    private enum CameraState {
        CLOSED, OPEN, PREVIEW, RECORDING, ERROR
    }

    private enum CameraType {
        FRONT, BACK
    }

    private CWCamera() {}

    private static class Singleton {
        public static final CWCamera instance = new CWCamera();
    }

    public static CWCamera getInstance() {
        return Singleton.instance;
    }

    public boolean initIfNeeded() {
        if(isInitted) {
            return true;
        }

        cameraType = CameraType.FRONT;
        openCameras();

        if(frontCamera == null) {
            if(backCamera == null) {
                cameraState = CameraState.ERROR;
                return false;
            }
            else {
                cameraType = CameraType.BACK;
            }
        }

        cameraState = CameraState.OPEN;

        if(frontCamera != null) {
            frontCamera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int i, Camera camera) {
                    Logger.e("Got an error from the front camera (error = " + i + ")");
                    cameraState = CameraState.ERROR;
                }
            });
        }
        if(backCamera != null && backCamera != frontCamera) {
            backCamera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int i, Camera camera) {
                    Logger.e("Got an error from the back camera (error = " + i + ")");
                    cameraState = CameraState.ERROR;
                }
            });
        }

        isInitted = true;
        return true;
    }

    public boolean canUse() {
        return isInitted && !hasError();
    }

    public boolean hasError() {
        return cameraState == CameraState.ERROR;
    }

    public boolean isShowingPreview() {
        return cameraState == CameraState.PREVIEW;
    }

    public boolean attachToPreview(Context context, SurfaceHolder surface, int width, int height) {
        if(!hasError() && getCurrentCamera() != null) {
            try {
                if(isShowingPreview()) {
                    getCurrentCamera().stopPreview();
                }

                getCurrentCamera().setPreviewDisplay(surface);
                setCameraPreviewSize(context, width, height);
                getCurrentCamera().startPreview();

                initMediaRecorder(context, width, height);

                cameraState = CameraState.PREVIEW;
                return true;
            }
            catch(Exception e) {
                Logger.e("Couldn't attach the camera to the preview", e);
                cameraState = CameraState.ERROR;
                return false;
            }
        }
        else {
            return false;
        }
    }

    public boolean startRecording() {
        return true;
    }

    private Camera getCurrentCamera() {
        if(cameraType == CameraType.FRONT) {
            return frontCamera;
        }
        else {
            return backCamera;
        }
    }

    private int getCurrentCameraId() {
        if(cameraType == CameraType.FRONT) {
            return frontCameraId;
        }
        else {
            return backCameraId;
        }
    }

    private void openCameras() {
        if(isInitted) {
            return;
        }

        int cameraCount = Camera.getNumberOfCameras();
        try {
            if(cameraCount > 1) {
                frontCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                frontCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                backCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                backCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                setInitialParameters(frontCamera);
                setInitialParameters(backCamera);
            }
            else {
                frontCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                backCamera = frontCamera;
                frontCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                backCameraId = frontCameraId;
                setInitialParameters(frontCamera);
            }
        }
        catch(Exception e) {
            Logger.e("Couldn't open the camera", e);
        }
    }

    private void initMediaRecorder(Context context, int width, int height) {
        recorder = new MediaRecorder();
        recorder.setCamera(getCurrentCamera());
        if(!setRecorderParams(context, width, height)) {
            releaseRecorder();
        }
    }

    private void setInitialParameters(Camera camera) {
        try {
            if(camera != null) {
                Camera.Parameters params = camera.getParameters();
                params.setRecordingHint(true);
                int[] bestFrameFrate = getBestCameraFrameRate(params.getSupportedPreviewFpsRange());
                params.setPreviewFpsRange(bestFrameFrate[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                                          bestFrameFrate[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
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

    private void setCameraPreviewSize(Context context, int width, int height) {
        if(getCurrentCamera() != null) {
            getCurrentCamera().stopPreview();
            Camera.Parameters params = getCurrentCamera().getParameters();
            Camera.Size bestSize = getBestSize(width, height, params.getSupportedPreviewSizes());

            if(bestSize != null) {
                params.setPreviewSize(bestSize.width, bestSize.height);
            }

            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            if (display.getRotation() == Surface.ROTATION_0) {
                getCurrentCamera().setDisplayOrientation(90);
            }
            if (display.getRotation() == Surface.ROTATION_270) {
                getCurrentCamera().setDisplayOrientation(180);
            }
        }
    }

    private boolean setRecorderParams(Context context, int width, int height) {
        if(getCurrentCamera() != null && recorder != null) {
            try {
                Camera.Parameters params = getCurrentCamera().getParameters();

                //must be before setOutputFormat
                recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

                //everything under here until prepare must be after setVideoSource and setOutputFormat
                int[] frameRateRange = new int[2];
                getCurrentCamera().getParameters().getPreviewFpsRange(frameRateRange);
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

                recorder.setVideoEncodingBitRate(CamcorderProfile.get(
                                                    getCurrentCameraId(), CamcorderProfile.QUALITY_HIGH).videoBitRate);

                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

                Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                int mrRotate = 0;

                if (display.getRotation() == Surface.ROTATION_0) {
                    mrRotate = 270;
                }
                else if (display.getRotation() == Surface.ROTATION_270) {
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

                File recordingFile = MessageDataStore.makeTempVideoFile();
                recorder.setOutputFile(recordingFile.getPath());

                recorder.prepare();

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

    private void releaseResources() {
        releaseCameras();
        releaseResources();
    }

    private void releaseCameras() {
        if(getCurrentCamera() != null) {
            if(isShowingPreview()) {
                getCurrentCamera().stopPreview();
            }
        }
        if(backCamera != frontCamera && backCamera != null) {
            backCamera.release();
            backCamera = null;
        }
        if(frontCamera != null) {
            frontCamera.release();
            frontCamera = null;
        }
    }

    private void releaseRecorder() {
        if(recorder != null) {
            recorder.reset();
            recorder.release();
            recorder = null;
        }
    }
}
