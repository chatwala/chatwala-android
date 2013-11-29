package co.touchlab.customcamera.util;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/27/13
 * Time: 11:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class VideoUtils
{
    public static synchronized Bitmap createVideoFrame(String filePath, long ms)
    {
        CWLog.i(VideoUtils.class, "filePath: " + filePath + "/ms: " + ms);

        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try
        {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        }
        catch (IllegalArgumentException ex)
        {
            CWLog.i(VideoUtils.class, "", ex);
        }
        catch (RuntimeException ex)
        {
            CWLog.i(VideoUtils.class, "", ex);
        }
        finally
        {
            try
            {
                retriever.release();
            }
            catch (RuntimeException ex)
            {
                CWLog.i(VideoUtils.class, "", ex);
            }
        }

        return bitmap;
    }

    public static class VideoMetadata
    {
        public File videoFile;
        public int height;
        public int width;
        public int duration;
    }

    public static VideoMetadata findMetadata(File videoFile) throws IOException
    {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        FileInputStream inp = new FileInputStream(videoFile);

        metaRetriever.setDataSource(inp.getFD());
        String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

        String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

        inp.close();
        VideoMetadata metadata = new VideoMetadata();

        metadata.videoFile = videoFile;
        metadata.height = Integer.parseInt(height);
        metadata.width = Integer.parseInt(width);
        metadata.duration = Integer.parseInt(duration);

        return metadata;
    }

}
