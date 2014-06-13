package com.chatwala.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import com.chatwala.android.R;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class CwButton extends FrameLayout {
    private FrameLayout actionContainer;

    public CwButton(Context context) {
        super(context);
        init(context);
    }

    public CwButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CwButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context) {
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        int defaultOuterSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        int outerOvalSize;
        boolean hideHalo = false;

        if(attrs == null) {
            outerOvalSize = defaultOuterSize;
        }
        else {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CwButton);
            if(a != null) {
                outerOvalSize = a.getDimensionPixelSize(R.styleable.CwButton_cw_button_size, 100);
                hideHalo = a.getBoolean(R.styleable.CwButton_cw_hide_halo, false);
            }
            else {
                outerOvalSize = defaultOuterSize;
            }
        }
        int innerOvalSize = (int) Math.round(outerOvalSize * .80);

        View haloView = new View(context);
        actionContainer = new FrameLayout(context);

        setLayoutParams(new LayoutParams(outerOvalSize, outerOvalSize));
        haloView.setLayoutParams(new LayoutParams(outerOvalSize, outerOvalSize, Gravity.CENTER));
        actionContainer.setLayoutParams(new LayoutParams(innerOvalSize, innerOvalSize, Gravity.CENTER));

        haloView.setBackgroundResource(R.drawable.cw_button_halo);
        actionContainer.setBackgroundResource(R.drawable.cw_button);

        for(int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if(child != null) {
                removeViewAt(i);
                actionContainer.addView(child);
            }
        }

        if(hideHalo) {
            haloView.setVisibility(GONE);
        }

        addView(haloView);
        addView(actionContainer);
    }

    public void setActionView(View actionView) {
        actionContainer.removeAllViews();
        actionView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        actionContainer.addView(actionView);
    }

    public void addActionView(View actionView) {
        addActionView(actionView, actionContainer.getChildCount() - 1);
    }

    public void addActionView(View actionView, int position) {
        actionView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        actionContainer.addView(actionView, position);
    }

    public void removeActionViewAt(int position) {
        actionContainer.removeViewAt(position);
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
