package com.chatwala.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import com.chatwala.android.database.ChatwalaMessage;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/9/13
 * Time: 6:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShareUtils
{

    public static final String EMAIL_CONTENT_PREFIX = "content://gmail-ls/";
    public static final String MARKET_STRING = "market://details?id=com.chatwala.chatwala&message=";
    public static final String WEB_STRING = "http://www.chatwala.com/?";
    public static final String ALT_WEB_STRING = "http://www.chatwala.com/#";

    public static ChatwalaMessage extractFileAttachmentFromUrl(Context context, String walaFileUrl)
    {
        return extractFileAttachment(context, Uri.fromFile(new File(walaFileUrl)));
    }

    public static ChatwalaMessage extractFileAttachmentFromIntent(Context context, Intent intent)
    {
        return extractFileAttachment(context, intent.getData());
    }

    private static ChatwalaMessage extractFileAttachment(Context activity, Uri uri)
    {
        if (uri != null)
        {
            try
            {
                InputStream is = activity.getContentResolver().openInputStream(uri);
                File file = new File(activity.getFilesDir(), "vid_" + System.currentTimeMillis() + ".wala");
                FileOutputStream os = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int count;

                while ((count = is.read(buffer)) > 0)
                    os.write(buffer, 0, count);

                os.close();
                is.close();

                File outFolder = new File(activity.getFilesDir(), "chat_" + System.currentTimeMillis());
                outFolder.mkdirs();

                ZipUtil.unzipFiles(file, outFolder);

                ChatwalaMessage chatwalaMessage = new ChatwalaMessage();
                chatwalaMessage.setMessageFile(new File(outFolder, "video.mp4"));
                FileInputStream input = new FileInputStream(new File(outFolder, "metadata.json"));
                chatwalaMessage.initMetadata(new JSONObject(IOUtils.toString(input)));

                input.close();

                return chatwalaMessage;
                        /*videoMonitorHandler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                triggerButtonAction();
                            }
                        }, 2000);*/
            }
            catch (FileNotFoundException e)
            {
                CWLog.b(ShareUtils.class, uri.toString());
                CWLog.softExceptionLog(ShareUtils.class, "Couldn't read file", e);
                return null;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            catch (JSONException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            return null;
        }
    }

    private static String dirtyEmailMagic(String s)
    {
        try
        {
            if(s.contains(EMAIL_CONTENT_PREFIX))
            {
                String email = s.substring(EMAIL_CONTENT_PREFIX.length(), s.indexOf('/', EMAIL_CONTENT_PREFIX.length()));
                if(!TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
                    return email;
            }
        }
        catch (Exception e)
        {
            //This appears kind of lazy, but we have no idea what kind of weird patterns we'll be getting in the future. Log and forget.
            CWLog.i(ShareUtils.class, "Failed extracting email from: "+ s, e);
        }

        return null;
    }

    public static String getIdFromIntent(Intent callingIntent)
    {
        String uri = callingIntent.getDataString();
        if(uri.startsWith(WEB_STRING))
            return uri.replace(WEB_STRING, "");
        else if(uri.startsWith(ALT_WEB_STRING))
            return uri.replace(ALT_WEB_STRING, "");
        else if(uri.startsWith(MARKET_STRING))
            return uri.replace(MARKET_STRING, "");
        else
            throw new RuntimeException("Invalid message id from intent");
    }

    /*public static Wala extractAttachment(Intent intent, Context context)
    {
        Uri uri = intent.getData();
        if (uri != null)
        {
            try
            {
                InputStream is = context.getContentResolver().openInputStream(uri);
                File file = new File(context.getFilesDir(), "vid_" + System.currentTimeMillis() + ".wala");
                FileOutputStream os = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int count;

                while ((count = is.read(buffer)) > 0)
                    os.write(buffer, 0, count);

                os.close();
                is.close();

                File outFolder = new File(context.getFilesDir(), "chat_" + System.currentTimeMillis());
                outFolder.mkdirs();

                ZipUtil.unzipFiles(file, outFolder);

                Wala wala = new Wala();
                wala.video = new File(outFolder, "video.mp4");
                FileInputStream input = new FileInputStream(new File(outFolder, "metadata.json"));
                JSONObject jsonObject = new JSONObject(IOUtils.toString(input));
                input.close();
                wala.startRecording = Math.round(jsonObject.getDouble("start_recording") * 1000d);

                return wala;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            catch (JSONException e)
            {
                throw new RuntimeException(e);
            }
        }

        return null;
    }*/
}
