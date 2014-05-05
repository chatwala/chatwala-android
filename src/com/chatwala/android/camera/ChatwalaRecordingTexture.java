package com.chatwala.android.camera;

import android.content.Context;
import android.view.TextureView;
import android.view.ViewGroup;

/**
 * Created by Eliezer on 4/23/2014.
 */
public class ChatwalaRecordingTexture extends TextureView {
    public ChatwalaRecordingTexture(Context context) {
        super(context);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        CWCamera camera = CWCamera.getInstance();
        if (getParent() != null) {
            ViewGroup parent = ((ViewGroup) getParent());
            if(parent.getHeight() != 0) {
                int viewWidth = ((ViewGroup) getParent()).getWidth();
                int previewHeight = getWidth();
                int previewWidth = getHeight();
                if(camera != null && camera.getPreviewSize() != null) {
                    previewHeight = camera.getPreviewSize().width;
                    previewWidth = camera.getPreviewSize().height;
                }

                if(previewWidth == 0 || previewHeight == 0 ) {
                    setMeasuredDimension(1, 1);
                    return;
                }

                double ratio = (double) viewWidth / (double) previewWidth;

                double newPreviewHeight = (double) previewHeight * ratio;
                double newPreviewWidth = (double) previewWidth * ratio;

                //Preview is rotated 90 degrees, so swap width/height
                setMeasuredDimension((int) newPreviewWidth, (int) newPreviewHeight);
            }
            else {
                //The surface needs a non-zero size for the callbacks to trigger
                setMeasuredDimension(1, 1);
            }
        }
        else {
            //The surface needs a non-zero size for the callbacks to trigger
            setMeasuredDimension(1, 1);
        }
    }
}
