package co.touchlab.customcamera.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import co.touchlab.customcamera.R;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 12/6/13
 * Time: 2:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class RippleTimer extends View
{
    private long start = System.currentTimeMillis();
    private Bitmap ripple;
    private int screenWidth;
    private int screenHeight;

    public RippleTimer(Context context)
    {
        super(context);
        init();
    }

    public RippleTimer(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public RippleTimer(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
        ripple = BitmapFactory.decodeResource(getResources(), R.drawable.ripple_overlay);
        Display display = ((Activity)getContext()).getWindow().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        long diff = (System.currentTimeMillis() - start);
        long yDiff = Math.max(diff - 500, 0l)/4;

        //Find First
        long xPos = 0 - diff;
        long minX = -ripple.getWidth();
        while(xPos < minX)
        {
            xPos += ripple.getWidth();
        }
        canvas.drawBitmap(ripple, xPos, 0 - yDiff, null);
        canvas.drawBitmap(ripple, ripple.getWidth() + xPos, 0 - yDiff, null);

//        if(yDiff > ripple.getHeight())
            invalidate();
    }
}
