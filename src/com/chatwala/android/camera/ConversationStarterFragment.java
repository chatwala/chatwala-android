package com.chatwala.android.camera;

import com.chatwala.android.util.Logger;

/**
 * Created by Eliezer on 4/23/2014.
 */
public class ConversationStarterFragment extends ChatwalaFragment {
    private CWCamera camera;
    private ChatwalaRecordingTexture recordingSurface;

    @Override
    public void onSurfaceCreated(ChatwalaRecordingTexture recordingSurface) {
        Logger.i("Surface is ready for ConversationStarterFragment");
        this.recordingSurface = recordingSurface;
        addViewToTop(recordingSurface, false);
    }

    @Override
    public void onCameraReady(CWCamera camera) {
        Logger.i("Camera is ready for ConversationStarterFragment");
        this.camera = camera;
    }

    @Override
    public void onActionButtonClicked() {

    }

    @Override
    protected void onTopFragmentClicked() {
        if(camera != null && !camera.hasError()) {
            if(camera.toggleCamera(recordingSurface.getWidth(), recordingSurface.getHeight())) {
                camera.attachToPreview(recordingSurface.getSurfaceTexture());
            }
        }
    }

    @Override
    protected void onBottomFragmentClicked() {

    }
}
