package com.chatwala.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/15/13
 * Time: 2:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class CroppingLayout extends ViewGroup {
    public CroppingLayout(Context context) {
        super(context);
    }

    public CroppingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CroppingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxHeight = 0;
        int maxWidth = 0;

        // Find out how big everyone wants to be
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        // Check against minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
                resolveSize(maxHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != null && child.getVisibility() != GONE) {
                int viewWidth = getMeasuredWidth();
                int viewHeight = getMeasuredHeight();

                int childLeft = -((child.getMeasuredWidth()/2) - (viewWidth/2));
                int childTop = -((child.getMeasuredHeight()/2) - (viewHeight/2));
                child.layout(childLeft, childTop,
                        childLeft + child.getMeasuredWidth(),
                        childTop + child.getMeasuredHeight());

            }
        }
    }
}
