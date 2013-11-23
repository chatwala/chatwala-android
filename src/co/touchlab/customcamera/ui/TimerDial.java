package co.touchlab.customcamera.ui;

import android.animation.*;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/23/13
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimerDial extends View
{
    private Integer countdown;
    private Integer playback;
    private Integer playbackRecording;
    private Integer record;
    private Integer currentTime;
    private int totalDuration;
    private TimerCallback callback;
    private boolean countdownComplete;
    private boolean playbackRecordingComplete;
    private boolean playbackComplete;
    private boolean recordComplete;
    private ValueAnimator valueAnimator;

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

    public interface TimerCallback
    {
        void countdownComplete();

        void playbackComplete();

        void recordStart();
        void recordComplete();
    }

    public void startAnimation(TimerCallback callback, Integer countdown, Integer playback, Integer playbackRecording, Integer record)
    {
        if(valueAnimator != null)
            return;

        this.countdownComplete = false;
        this.playbackRecordingComplete = false;
        this.playbackComplete = false;
        this.recordComplete = false;

        this.callback = callback;
        this.countdown = countdown;
        this.playback = playback;
        this.playbackRecording = playbackRecording;
        this.record = record;
        this.totalDuration = countdown + playback + record;

        valueAnimator = ValueAnimator.ofFloat(0, 1);

        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setDuration(totalDuration);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                TimerDial.this.currentTime = (int) ((Float) animation.getAnimatedValue() * (float) totalDuration);
                if (!countdownComplete && currentTime > TimerDial.this.countdown)
                {
                    countdownComplete = true;
                    TimerDial.this.callback.countdownComplete();
                }
                if(!playbackRecordingComplete && currentTime > TimerDial.this.playbackRecording + TimerDial.this.countdown)
                {
                    playbackRecordingComplete = true;
                    TimerDial.this.callback.recordStart();
                }
                if (!playbackComplete && currentTime > TimerDial.this.playback + TimerDial.this.countdown)
                {
                    playbackComplete = true;
                    TimerDial.this.callback.playbackComplete();
                }

                invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                resetValues();
                invalidate();
                TimerDial.this.callback.recordComplete();
            }
        });

        valueAnimator.start();
    }

    private void resetValues()
    {
        countdown = null;
        playback = null;
        playbackRecording = null;
        record = null;
        currentTime = null;
        totalDuration = 0;
        countdownComplete = false;
        playbackComplete = false;
        playbackRecordingComplete = false;
        recordComplete = false;
        valueAnimator = null;
    }

    public void stopAnimation()
    {
        valueAnimator.cancel();
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

        if (totalDuration > 0)
        {
            int playbackStartTime = countdown;
            int playbackRecordingStartTime = countdown + playbackRecording;
            int recordStartTime = countdown + playback;

            if (currentTime < countdown)
            {
                Paint countdownPaint = new Paint();

                countdownPaint.setColor(Color.GRAY);
                countdownPaint.setStyle(Paint.Style.FILL);

                int startArc = findArcPoint(currentTime.longValue(), countdown.longValue());
                int endArc = 360;

                canvas.drawArc(arcRect, startArc, endArc - startArc, true, countdownPaint);
            }
            else
            {
                if (currentTime < playbackRecordingStartTime)
                {
                    Paint countdownPaint = new Paint();

                    countdownPaint.setColor(Color.CYAN);
                    countdownPaint.setStyle(Paint.Style.FILL);

                    int arcPoint = Math.max(currentTime.intValue(), playbackStartTime);
                    int startArc = findArcPoint(arcPoint);
                    int endArc = findArcPoint(playbackRecordingStartTime);

                    canvas.drawArc(arcRect, startArc, endArc - startArc, true, countdownPaint);
                }
                if (currentTime < recordStartTime)
                {
                    Paint countdownPaint = new Paint();

                    countdownPaint.setColor(Color.GREEN);
                    countdownPaint.setStyle(Paint.Style.FILL);

                    int arcPoint = Math.max(currentTime.intValue(), (int) playbackRecordingStartTime);
                    int startArc = findArcPoint(arcPoint);
                    int endArc = findArcPoint(recordStartTime);

                    canvas.drawArc(arcRect, startArc, endArc - startArc, true, countdownPaint);
                }

                Paint recordPaint = new Paint();

                recordPaint.setColor(Color.RED);
                recordPaint.setStyle(Paint.Style.FILL);

                int arcPoint = Math.max(currentTime.intValue(), (int) recordStartTime);
                int startArc = findArcPoint(arcPoint);
                int endArc = findArcPoint(totalDuration);

                Log.w("arc", "startArc: " + startArc + "/endArc: " + endArc);
                canvas.drawArc(arcRect, startArc, endArc - startArc, true, recordPaint);
            }
        }
    }

    private int findArcPoint(long arcPoint)
    {
        return findArcPoint(arcPoint - countdown, totalDuration - countdown);
    }

    private int findArcPoint(long current, long total)
    {
        return (int) (((double)current/(double)total) * 360d);
    }
}
