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
    private View modifiableView;

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

        View transparentOval = new View(context);
        View modifiableView;
        if(drawableRes != null) {
            modifiableView = new ImageView(context);
            ((ImageView) modifiableView).setImageDrawable(drawableRes);
            ((ImageView) modifiableView).setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
        else if(text != null) {
            modifiableView = new TextView(context);
            ((TextView) modifiableView).setGravity(Gravity.CENTER);
            ((TextView) modifiableView).setText(text);
            ((TextView) modifiableView).setTextColor(0);
            ((TextView) modifiableView).setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
            ((TextView) modifiableView).setTypeface(Typeface.DEFAULT_BOLD);
        }
        else {
            modifiableView = new View(context);
        }
        int innerOvalSize = (int) Math.round(outerOvalSize * .80);

        setLayoutParams(new LayoutParams(outerOvalSize, outerOvalSize));
        transparentOval.setLayoutParams(new LayoutParams(outerOvalSize, outerOvalSize, Gravity.CENTER));
        modifiableView.setLayoutParams(new LayoutParams(innerOvalSize, innerOvalSize, Gravity.CENTER));

        transparentOval.setBackgroundResource(R.drawable.cw_button_halo);
        modifiableView.setBackgroundResource(R.drawable.cw_button);

        addView(transparentOval);
        addView(modifiableView);
    }

    public View getModifiableView() {
        return modifiableView;
    }
}
