package com.chatwala.android.camera;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Created by Eliezer on 4/18/2014.
 */
public class AcquireCameraAsyncTask extends AsyncTask<Void, Void, CWCamera> {
    private Context context;
    private OnCameraReadyListener listener;
    private int width;
    private int height;

    public interface OnCameraReadyListener {
        public void onCameraReady(CWCamera camera);
    }

    public AcquireCameraAsyncTask(Context context, OnCameraReadyListener listener, int width, int height) {
        this.context = context;
        this.listener = listener;
        this.width = width;
        this.height = height;
    }

    @Override
    protected CWCamera doInBackground(Void... voids) {
        CWCamera camera = CWCamera.getInstance();
        camera.init(context, width, height);
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
