package com.chatwala.android.http;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.ThumbUtils;
import com.chatwala.android.util.VideoUtils;
import com.turbomanage.httpclient.HttpResponse;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 1/3/14
 * Time: 2:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class PutUserProfilePictureRequest extends BaseSasPutRequest
{
    String filePath;
    Boolean isPicture;

    public PutUserProfilePictureRequest(Context context, String filePath, boolean isPicture)
    {
        super(context);
        this.filePath = filePath;
        this.isPicture = isPicture;
    }

    @Override
    protected String getResourceURL()
    {
        return "users/" + AppPrefs.getInstance(context).getUserId() + "/pictureUploadURL";
    }

    @Override
    protected byte[] getBytesToPut()
    {
        if(isPicture)
        {
            try
            {
                return FileUtils.readFileToByteArray(new File(filePath));
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            Log.d("#######", "Getting the put data for " + filePath);
            Bitmap frame = VideoUtils.createVideoFrame(filePath, 1);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            frame.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] toReturn = stream.toByteArray();

            InputStream is = new ByteArrayInputStream(stream.toByteArray());
            File file = MessageDataStore.findUserImageInLocalStore(AppPrefs.getInstance(context).getUserId());
            try
            {
                FileOutputStream os = new FileOutputStream(file);


                final byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }

                os.close();
                is.close();
            }
            catch (FileNotFoundException e)
            {
                throw new RuntimeException(e);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            return toReturn;
        }
    }

    @Override
    protected void onPutSuccess(DatabaseHelper databaseHelper) throws SQLException
    {
        if(!isPicture)
        {
            new File(filePath).delete();
        }

        ThumbUtils.createThumbForUserImage(context, AppPrefs.getInstance(context).getUserId());
    }

    @Override
    protected boolean isPngImage()
    {
        return true;
    }
}
