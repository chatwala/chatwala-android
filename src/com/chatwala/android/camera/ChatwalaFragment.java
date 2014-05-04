package com.chatwala.android.camera;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.R;
import com.chatwala.android.ui.CroppingLayout;
import com.chatwala.android.util.DimenUtils;

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

    protected AppPrefs getPrefs() {
        return getCwActivity().getPrefs();
    }

    public abstract void onCameraReady(CWCamera camera);

    public abstract void onActionButtonClicked();

    protected abstract void onTopFragmentClicked();

    protected abstract void onBottomFragmentClicked();

    protected abstract void onRecordingFinished(RecordingInfo recordingInfo);

    protected void addViewToTop(View v, boolean popTop) {
        addView(v, popTop, topContainer);
    }

    protected void addViewToBottom(View v, boolean popTop) {
        addView(v, popTop, bottomContainer);
    }

    protected void setTopView(View v) {
        topContainer.removeAllViews();
        addView(v, false, topContainer);
    }

    protected void setBottomView(View v) {
        bottomContainer.removeAllViews();
        addView(v, false, bottomContainer);
    }

    private void addView(View v, boolean popTop, ViewGroup container) {
        if(popTop) {
            container.removeViewAt(container.getChildCount() - 1);
        }

        container.addView(v);
    }

    protected TextView generateCwTextView(int textRes, int backgroundColor) {
        return generateCwTextView(getString(textRes), backgroundColor);
    }

    protected TextView generateCwTextView(String text, int backgroundColor) {
        Resources r = getResources();
        TextView tv = new TextView(getActivity());
        tv.setTypeface(getApp().fontMd);
        tv.setBackgroundColor(Color.TRANSPARENT);
        tv.setGravity(Gravity.CENTER);
        tv.setText(text);
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(DimenUtils.convertPixelsToDp(r.getDimension(R.dimen.message_text_size), getActivity()));
        tv.setLineSpacing(DimenUtils.convertPixelsToDp(r.getDimension(R.dimen.message_text_spacing), getActivity()), 1);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) DimenUtils.convertPixelsToDp(20, getActivity());
        lp.setMargins(margin, margin, margin, margin);
        lp.gravity = Gravity.CENTER;
        tv.setLayoutParams(lp);

        return tv;
    }
}
