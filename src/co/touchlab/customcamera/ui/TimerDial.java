package co.touchlab.customcamera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/23/13
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimerDial extends View
{
    public TimerDial(Context context)
    {
        super(context);
    }

    public TimerDial(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public TimerDial(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        RectF arcRect = new RectF(0, 0, getWidth(), getHeight());

        Paint clearPaint = new Paint();

        clearPaint.setColor(Color.BLACK);
        clearPaint.setStyle(Paint.Style.FILL);

        canvas.drawOval(arcRect, clearPaint);
    }
}
