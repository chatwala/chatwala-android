package com.chatwala.android.camera;

import android.os.AsyncTask;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class AcquireCameraAsyncTask extends AsyncTask<Void, Void, CwCamera> {
    private OnCameraReadyListener listener;
    private int width;
    private int height;

    public interface OnCameraReadyListener {
        public void onCameraReady(CwCamera camera);
    }

    public AcquireCameraAsyncTask(OnCameraReadyListener listener, int width, int height) {
        this.listener = listener;
        this.width = width;
        this.height = height;
    }

    @Override
    protected CwCamera doInBackground(Void... voids) {
        CwCamera camera = CwCamera.getInstance();
        camera.init(width, height);
        return camera;
    }

    @Override
    public void onPostExecute(CwCamera camera) {
        if(isCancelled()) {
            camera.release();
        }
        else if(listener != null) {
            listener.onCameraReady(camera);
        }
    }
}
