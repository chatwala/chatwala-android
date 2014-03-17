package com.chatwala.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Eliezer on 3/17/14.
 */
public class ContactImageView extends ImageView {
    public ContactImageView(Context context) {
        super(context);
    }

    public ContactImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth()); //Snap to width
    }
}
