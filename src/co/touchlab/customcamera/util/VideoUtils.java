package co.touchlab.customcamera.util;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;

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
            bitmap = retriever.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
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
}
