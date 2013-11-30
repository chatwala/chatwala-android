package co.touchlab.customcamera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import co.touchlab.customcamera.R;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/23/13
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimerDial extends View
{
    Integer playOnlyDuration;
    Integer endRecordTime;
    int currentOffset;
    private float timerBarSize;
    private Paint baseRedPaint;
    private Paint clearPaint;
    private Paint lightRedPaint;

    public TimerDial(Context context)
    {
        super(context);
        init();
    }

    public TimerDial(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public TimerDial(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
        timerBarSize = getResources().getDimension(R.dimen.timer_bar_size);

        clearPaint = new Paint();

        clearPaint.setColor(Color.WHITE);
        clearPaint.setAlpha(100);
        clearPaint.setStyle(Paint.Style.FILL);

        baseRedPaint = new Paint();

        baseRedPaint.setColor(getResources().getColor(R.color.dot_red));
        baseRedPaint.setStyle(Paint.Style.FILL);

        lightRedPaint = new Paint();

        lightRedPaint.setColor(getResources().getColor(R.color.dot_red_light));
        lightRedPaint.setStyle(Paint.Style.FILL);
    }

    public void resetAnimation(Integer playOnlyDuration, Integer endRecordTime)
    {
        currentOffset = 0;
        this.playOnlyDuration = playOnlyDuration;
        this.endRecordTime = endRecordTime;
        invalidate();
    }

    public void updateOffset(int currentOffset)
    {
        this.currentOffset = currentOffset;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        RectF outerArcRect = new RectF(0, 0, getWidth(), getHeight());
        RectF innerArcRect = new RectF(timerBarSize, timerBarSize, getWidth() - timerBarSize, getHeight() - timerBarSize);

        canvas.drawOval(outerArcRect, clearPaint);

        if(endRecordTime != null)
        {
            int playOnly = playOnlyDuration == null ? 0 : playOnlyDuration;

            int recordDuration = endRecordTime - playOnly;

            int recordAngle;

            if(currentOffset <= playOnly)
            {
                recordAngle = 0;
            }
            else
            {
                int currentRecordOffset = currentOffset - playOnly;
                recordAngle = (int)(((double)currentRecordOffset/(double) recordDuration) * (double) 360);
            }

            //Draw remainder base red because drawing arcs over ovals didn't exactly match up.
            if(recordAngle > 0)
            {
                canvas.drawArc(innerArcRect, 0, (float)recordAngle, true, lightRedPaint);
                canvas.drawArc(innerArcRect, (float)recordAngle, 360-recordAngle, true, baseRedPaint);
            }
            else
            {
                canvas.drawOval(innerArcRect, baseRedPaint);
            }
        }
        else
        {
            canvas.drawOval(innerArcRect, baseRedPaint);
        }


    }
}
