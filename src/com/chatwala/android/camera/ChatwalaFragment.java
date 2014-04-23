package com.chatwala.android.camera;

import android.support.v4.app.Fragment;
import com.chatwala.android.ChatwalaApplication;

public abstract class ChatwalaFragment extends Fragment {
    protected ChatwalaApplication getApp() {
        return getCwActivity().getApp();
    }

    protected ChatwalaActivity getCwActivity() {
        return ((ChatwalaActivity) getActivity());
    }

    public abstract void onCameraReady();

    public abstract void onActionButtonClicked();
}
