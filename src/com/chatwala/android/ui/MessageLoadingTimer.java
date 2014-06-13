package com.chatwala.android.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.chatwala.android.R;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class MessageLoadingTimer extends RelativeLayout {
    private static final float WAVE = 100;

    private ImageView rippleView;
    private ImageView iconView;
    private boolean shouldMakeWaves = true;
    private int progress = 0;
    private boolean canceled = false;

    private OnTimerFinishedListener listener;

    public interface OnTimerFinishedListener {
        public void onLoadComplete();
    }

    public MessageLoadingTimer(Context context) {
        super(context);
        init();
    }

    public MessageLoadingTimer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MessageLoadingTimer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.message_loading_timer, this);
        rippleView = (ImageView) findViewById(R.id.ripple_overlay);
        iconView = (ImageView) findViewById(R.id.ripple_chatwala_icon);
        reset();
    }

    public void setOnTimerFinishedListener(OnTimerFinishedListener listener) {
        this.listener = listener;
    }

    public void cancel() {
        this.canceled = true;
    }

    public void reset() {
        canceled = false;
        shouldMakeWaves = true;
        makeWaves(WAVE);
        setProgress(1);
    }

    public void setProgress(final int progress) {
        if(progress > 100) {
            shouldMakeWaves = false;
            if(listener != null) {
                listener.onLoadComplete();
            }
            return;
        }

        this.progress = progress;
    }

    private void makeWaves(final float translationX) {
        ViewPropertyAnimator animator = rippleView.animate();
        if(animator != null) {
            float translationY = -(((float) progress / 100) * iconView.getMeasuredHeight());
            if(translationX > 0) {
                translationY += 5;
            }
            else {
                translationY -= 5;
            }
            animator.setDuration(500)
                    .translationXBy(translationX)
                    .translationY(translationY)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    if (progress < 100 && shouldMakeWaves && !canceled) {
                                        makeWaves(-translationX);
                                    }
                                }
                            });
                        }
                    })
                    .start();
        }
    }

}