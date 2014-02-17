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
    public static final String ALT_WEB_STRING = "http://chatwala.com/?";
    public static final String DEV_WEB_STRING = "http://chatwala.com/dev/?";
    public static final String QA_WEB_STRING = "http://chatwala.com/qa/?";
    public static final String HASH_STRING = "http://www.chatwala.com/#";
    public static final String ALT_HASH_STRING = "http://chatwala.com/#";
    public static final String REDIRECT_STRING = "http://www.chatwala.com/droidredirect.html?";

    public static ChatwalaMessage extractFileAttachment(Activity activity, String walaFileUrl)
    {
        Uri uri = Uri.fromFile(new File(walaFileUrl));

        if (uri != null)
        {
            try
            {
                InputStream is = activity.getContentResolver().openInputStream(uri);
                File file = MessageDataStore.makeTempWalaFile();
                FileOutputStream os = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int count;

                while ((count = is.read(buffer)) > 0)
                    os.write(buffer, 0, count);

                os.close();
                is.close();

                File outFolder = MessageDataStore.makeTempChatDir();
                outFolder.mkdirs();

                ZipUtil.unzipFiles(file, outFolder);

                ChatwalaMessage chatwalaMessage = new ChatwalaMessage();
                chatwalaMessage.setMessageFile(MessageDataStore.makeVideoFile(outFolder));
                FileInputStream input = new FileInputStream(MessageDataStore.makeMetadataFile(outFolder));
                chatwalaMessage.initMetadata(activity, new JSONObject(IOUtils.toString(input)));

                input.close();

                return chatwalaMessage;
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
        CWLog.logShareLink(uri);
        if(uri.startsWith(WEB_STRING))
            return uri.replace(WEB_STRING, "");
        else if(uri.startsWith(ALT_WEB_STRING))
            return uri.replace(ALT_WEB_STRING, "");
        else if(uri.startsWith(DEV_WEB_STRING))
            return uri.replace(DEV_WEB_STRING, "");
        else if(uri.startsWith(QA_WEB_STRING))
            return uri.replace(QA_WEB_STRING, "");
        else if(uri.startsWith(REDIRECT_STRING))
            return uri.replace(REDIRECT_STRING, "");
        else if(uri.startsWith(MARKET_STRING))
            return uri.replace(MARKET_STRING, "");
        else if(uri.startsWith(HASH_STRING))
            return uri.replace(HASH_STRING, "");
        else if(uri.startsWith(ALT_HASH_STRING))
            return uri.replace(ALT_HASH_STRING, "");
        else
            throw new RuntimeException("Invalid message id from intent");
    }
}
