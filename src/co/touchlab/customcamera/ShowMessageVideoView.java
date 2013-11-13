package co.touchlab.customcamera;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/9/13
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShowMessageVideoView extends VideoView
{
    private File messageFile;
//    private MediaController mc;

    public ShowMessageVideoView(Context context)
    {
        super(context);
    }

    public ShowMessageVideoView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ShowMessageVideoView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    public void setMessageFile(File messageFile)
    {
        this.messageFile = messageFile;
    }

    public void playVideo()
    {
//        mc = new MediaController(getContext());
//        mc.setAnchorView(this);
//        mc.setMediaPlayer(this);
//
//        this.setMediaController(mc);
//        videoView.setZOrderMediaOverlay(false);
        this.setVideoPath(messageFile.getPath());

//        videoView.setRotation(180);
        this.start();
//        this.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
//        {
//            @Override
//            public void onCompletion(MediaPlayer mp)
//            {
//                playbackDone();
//            }
//        });
    }
}
