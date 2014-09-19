package com.chatwala.android.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.chatwala.android.R;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class MessageLoadingTimerFragment extends Fragment {
    private MessageLoadingTimer messageLoadingTimer;
    private MessageLoadingTimer.OnTimerFinishedListener listener;
    private Handler waitForTimerViewHandler;

    public MessageLoadingTimerFragment() {
        waitForTimerViewHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.message_loading_fragment, null);
        messageLoadingTimer = (MessageLoadingTimer) v.findViewById(R.id.ripple_timer);
        messageLoadingTimer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(getActivity() instanceof MessageLoadingTimer.OnTimerFinishedListener) {
            listener = (MessageLoadingTimer.OnTimerFinishedListener) getActivity();
        }
        else if(getTargetFragment() != null && getTargetFragment() instanceof MessageLoadingTimer.OnTimerFinishedListener) {
            listener = (MessageLoadingTimer.OnTimerFinishedListener) getTargetFragment();
        }

        if(listener != null) {
            messageLoadingTimer.setOnTimerFinishedListener(listener);
        }
    }

    public MessageLoadingTimer getMessageLoadingTimer() {
        return messageLoadingTimer;
    }

    public void reset() {
        if(messageLoadingTimer == null) {
            waitForTimerViewHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    reset();
                }
            }, 500);
            return;
        }
        messageLoadingTimer.reset();
    }

    public void setProgress(final int progress) {
        if(messageLoadingTimer == null) {
            waitForTimerViewHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setProgress(progress);
                }
            }, 500);
            return;
        }
        messageLoadingTimer.setProgress(progress);
    }

    public void cancel() {
        if(messageLoadingTimer == null) {
            waitForTimerViewHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    cancel();
                }
            }, 500);
            return;
        }
        messageLoadingTimer.cancel();
    }
}
