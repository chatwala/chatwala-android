package com.chatwala.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.chatwala.android.R;

/**
 * Created by Eliezer on 4/1/2014.
 */
public class CWButton extends FrameLayout {
    private FrameLayout actionContainer;

    public CWButton(Context context) {
        super(context);
        init(context);
    }

    public CWButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CWButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context) {
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        int defaultOuterSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        int outerOvalSize;
        Drawable drawableRes = null;
        String text = null;

        if(attrs == null) {
            outerOvalSize = defaultOuterSize;
        }
        else {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CWButton);
            if(a != null) {
                outerOvalSize = a.getDimensionPixelSize(R.styleable.CWButton_cw_button_size, 100);
                drawableRes = a.getDrawable(R.styleable.CWButton_cw_button_drawable);
                text = a.getString(R.styleable.CWButton_cw_button_text);
            }
            else {
                outerOvalSize = defaultOuterSize;
            }
        }
        int innerOvalSize = (int) Math.round(outerOvalSize * .80);

        View transparentOval = new View(context);
        actionContainer = new FrameLayout(context);

        View actionView = null;
        if(drawableRes != null) {
            actionView = new ImageView(context);
            ((ImageView) actionView).setImageDrawable(drawableRes);
            ((ImageView) actionView).setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
        else if(text != null) {
            actionView = new TextView(context);
            ((TextView) actionView).setGravity(Gravity.CENTER);
            ((TextView) actionView).setText(text);
            ((TextView) actionView).setTextColor(0);
            ((TextView) actionView).setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
            ((TextView) actionView).setTypeface(Typeface.DEFAULT_BOLD);
        }

        setLayoutParams(new LayoutParams(outerOvalSize, outerOvalSize));
        transparentOval.setLayoutParams(new LayoutParams(outerOvalSize, outerOvalSize, Gravity.CENTER));
        actionContainer.setLayoutParams(new LayoutParams(innerOvalSize, innerOvalSize, Gravity.CENTER));

        if(actionView != null) {
            actionView.setLayoutParams(new LayoutParams(innerOvalSize, innerOvalSize, Gravity.CENTER));
            actionContainer.addView(actionView);
        }

        transparentOval.setBackgroundResource(R.drawable.cw_button_halo);
        actionContainer.setBackgroundResource(R.drawable.cw_button);

        addView(transparentOval);
        addView(actionContainer);
    }

    public void setActionView(View actionView) {
        actionContainer.removeAllViews();
        actionContainer.addView(actionView);
    }

    public void addActionView(View actionView) {
        actionContainer.addView(actionView);
    }

    public void clearActionView() {
        actionContainer.removeAllViews();
    }

    public View getActionView() {
        return getActionViewAt(0);
    }

    public View getActionViewAt(int i) {
        return actionContainer.getChildAt(i);
    }
}
