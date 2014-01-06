package com.chatwala.android.http;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.util.VideoUtils;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/3/14
 * Time: 2:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class PutUserProfilePictureRequest extends BasePutRequest
{
    String videoPath;

    public PutUserProfilePictureRequest(Context context, String videoPath)
    {
        super(context);
        this.videoPath = videoPath;
    }

    @Override
    String getContentType()
    {
        return "image/png";
    }

    @Override
    protected byte[] getPutData()
    {
        Log.d("#######", "Getting the put data for " + videoPath);
        Bitmap frame = VideoUtils.createVideoFrame(videoPath, 1);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        frame.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    @Override
    protected String getResourceURL()
    {
        return "users/" + AppPrefs.getInstance(context).getUserId() + "/picture";
    }

    @Override
    protected void parseResponse(HttpResponse response) throws JSONException, SQLException
    {
        AppPrefs.getInstance(context).setSentEmail(true);
        new File(videoPath).delete();
    }
}
