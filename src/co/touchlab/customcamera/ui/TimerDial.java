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
    Integer recordDuration;
    int currentOffset;
    private float timerBarSize;

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
    }

    public void resetAnimation(Integer playOnlyDuration, Integer recordDuration)
    {
        currentOffset = 0;
        this.playOnlyDuration = playOnlyDuration;
        this.recordDuration = recordDuration;
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

        RectF arcRect = new RectF(timerBarSize, timerBarSize, getWidth() - timerBarSize, getHeight() - timerBarSize);

        if(recordDuration != null)
        {
            RectF timerArcRect = new RectF(0, 0, getWidth(), getHeight());
            int playOnly = playOnlyDuration == null ? 0 : playOnlyDuration;

            int totalDuration = playOnly + recordDuration;

            int playOnlyAngleTotal = (playOnly * 360) / totalDuration;
            int recordAngleTotal = 360 - playOnlyAngleTotal;

            int playOnlyAngle;

            if(currentOffset >= playOnly)
            {
                playOnlyAngle = playOnlyAngleTotal;
            }
            else
            {
                playOnlyAngle = (int)(((double)currentOffset/(double)playOnly) * (double) playOnlyAngleTotal);
            }

            if(playOnlyAngle > 0)
            {
                Paint playPaint = new Paint();
                playPaint.setColor(Color.GRAY);
                playPaint.setStyle(Paint.Style.FILL);
                canvas.drawArc(timerArcRect, 0f, (float)playOnlyAngle, true, playPaint);
            }

            int recordAngle;

            if(currentOffset <= playOnly)
            {
                recordAngle = 0;
            }
            else
            {
                int currentRecordOffset = currentOffset - playOnly;
                recordAngle = (int)(((double)currentRecordOffset/(double)recordDuration) * (double) recordAngleTotal);
            }

            if(recordAngle > 0)
            {
                Paint playPaint = new Paint();
                playPaint.setColor(Color.RED);
                playPaint.setStyle(Paint.Style.FILL);
                canvas.drawArc(timerArcRect, playOnlyAngleTotal, (float)recordAngle, true, playPaint);
            }
        }

        Paint clearPaint = new Paint();

        clearPaint.setColor(Color.BLACK);
        clearPaint.setStyle(Paint.Style.FILL);

        canvas.drawOval(arcRect, clearPaint);
    }
}
