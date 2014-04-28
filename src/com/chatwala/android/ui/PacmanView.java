package com.chatwala.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import com.chatwala.android.R;
import com.chatwala.android.util.Logger;

/**
 * Created by Eliezer on 4/28/2014.
 */
public class PacmanView extends View {
    private float mCurrAngle;
    private Paint lightRed;
    private RectF ovalBoundsF;
    private Rect ovalBounds;
    private OpenPacman animation;

    public PacmanView(Context context) {
        super(context);
        init();
    }

    public PacmanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PacmanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        lightRed = new Paint();
        lightRed.setColor(getResources().getColor(R.color.dot_red_light));
        lightRed.setAntiAlias(true);
        ovalBoundsF = new RectF();
        ovalBounds = new Rect();
    }

    public void startAnimation(long duration) {
        startAnimation(duration, null);
    }

    public void startAnimation(long duration, Animation.AnimationListener listener) {
        animation = new OpenPacman(0, 360, duration);
        animation.setAnimationListener(listener);
        startAnimation(animation);
        invalidate();
    }

    public void stopAndRemove() {
        if(animation != null) {
            animation.cancel();
        }
        try {
            ((ViewGroup) getParent()).removeView(this);
        }
        catch(Exception e) {
            Logger.e("Couldn't remove the PacmanView from its parent", e);
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        canvas.getClipBounds(ovalBounds);
        ovalBoundsF.set(ovalBounds);
        if(mCurrAngle >= 360) {
            lightRed.setColor(Color.WHITE);
            lightRed.setAlpha(125);
        }
        canvas.drawArc(ovalBoundsF, -90, mCurrAngle, true, lightRed);
    }

    public class OpenPacman extends Animation {
        float mStartAngle;
        float mSweepAngle;

        public OpenPacman (int startAngle, int sweepAngle, long duration) {
            mStartAngle = startAngle;
            mSweepAngle = sweepAngle;
            setDuration(duration);
            setInterpolator(new LinearInterpolator());
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            PacmanView.this.mCurrAngle = mStartAngle + ((mSweepAngle - mStartAngle) * interpolatedTime);
            PacmanView.this.invalidate();
        }
    }
}
