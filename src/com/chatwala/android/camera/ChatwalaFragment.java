package com.chatwala.android.camera;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.R;
import com.chatwala.android.ui.CroppingLayout;

public abstract class ChatwalaFragment extends Fragment {
    private CroppingLayout topContainer;
    private FrameLayout bottomContainer;

    protected ChatwalaApplication getApp() {
        return getCwActivity().getApp();
    }

    protected ChatwalaActivity getCwActivity() {
        return ((ChatwalaActivity) getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.chatwala_fragment, container, false);
        topContainer = (CroppingLayout) v.findViewById(R.id.chatwala_fragment_top);
        bottomContainer = (FrameLayout) v.findViewById(R.id.chatwala_fragment_bottom);

        topContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTopFragmentClicked();
            }
        });

        bottomContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBottomFragmentClicked();
            }
        });
        return v;
    }

    public abstract void onSurfaceCreated(ChatwalaRecordingTexture recordingSurface);

    public abstract void onCameraReady(CWCamera camera);

    public abstract void onActionButtonClicked();

    protected abstract void onTopFragmentClicked();

    protected abstract void onBottomFragmentClicked();

    protected void addViewToTop(View v, boolean popTop) {
        addView(v, popTop, topContainer);
    }

    protected void addViewToBottom(View v, boolean popTop) {
        addView(v, popTop, bottomContainer);
    }

    private void addView(View v, boolean popTop, ViewGroup container) {
        if(popTop) {
            container.removeViewAt(container.getChildCount() - 1);
        }

        container.addView(v);
    }
}
