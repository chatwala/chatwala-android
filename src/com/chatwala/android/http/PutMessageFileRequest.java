package com.chatwala.android.http;

import android.content.Context;
import com.chatwala.android.util.CameraUtils;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class PutMessageFileRequest extends BasePutRequest
{
    String localMessageUrl;
    String messageId;


    public PutMessageFileRequest(Context context, String localMessageUrl, String messageId)
    {
        super(context);
        this.localMessageUrl = localMessageUrl;
        this.messageId = messageId;
    }

    @Override
    protected String getResourceURL()
    {
        return "messages/" + messageId;
    }

    @Override
    protected byte[] getPutData()
    {
        File walaFile = new File(localMessageUrl);

        try
        {
            FileInputStream fileInputStream = new FileInputStream(walaFile);

            byte[] bFile = new byte[(int) walaFile.length()];

            try {
                //convert file into array of bytes
                fileInputStream = new FileInputStream(walaFile);
                fileInputStream.read(bFile);
                fileInputStream.close();

                for (int i = 0; i < bFile.length; i++) {
                    System.out.print((char)bFile[i]);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            return bFile;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
