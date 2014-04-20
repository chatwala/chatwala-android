package com.chatwala.android.camera;

import android.os.AsyncTask;

/**
 * Created by Eliezer on 4/18/2014.
 */
public class AcquireCameraAsyncTask extends AsyncTask<Void, Void, CWCamera> {
    private OnCameraReadyListener listener;

    public interface OnCameraReadyListener {
        public void onCameraReady(CWCamera camera);
    }

    public AcquireCameraAsyncTask(OnCameraReadyListener listener) {
        this.listener = listener;
    }

    @Override
    protected CWCamera doInBackground(Void... voids) {
        CWCamera camera = CWCamera.getInstance();
        camera.initIfNeeded();
        return camera;
    }

    @Override
    public void onPostExecute(CWCamera camera) {
        if(isCancelled()) {
            //camera.release
        }
        else if(listener != null) {
            listener.onCameraReady(camera);
        }
    }
}
