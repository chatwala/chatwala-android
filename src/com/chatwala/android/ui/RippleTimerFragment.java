package com.chatwala.android.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.chatwala.android.R;

public class RippleTimerFragment extends Fragment {
    private RippleTimer rippleTimer;
    private RippleTimer.OnTimerFinishedListener listener;

    public RippleTimerFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.splash_ripple, null);
        rippleTimer = (RippleTimer) v.findViewById(R.id.ripple_timer);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(getActivity() instanceof RippleTimer.OnTimerFinishedListener) {
            listener = (RippleTimer.OnTimerFinishedListener) getActivity();
        }
        else if(getTargetFragment() != null && getTargetFragment() instanceof RippleTimer.OnTimerFinishedListener) {
            listener = (RippleTimer.OnTimerFinishedListener) getTargetFragment();
        }

        if(listener != null) {
            rippleTimer.setOnTimerFinishedListener(listener);
        }
    }

    public RippleTimer getRippleTimer() {
        return rippleTimer;
    }

    public void reset() {
        rippleTimer.reset();
    }

    public void setProgress(int progress) {
        rippleTimer.setProgress(progress);
    }

    public void cancel() {
        rippleTimer.cancel();
    }
}
