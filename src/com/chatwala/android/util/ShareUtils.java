package com.chatwala.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import com.chatwala.android.CWResult;
import com.chatwala.android.EnvironmentVariables;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.networking.NetworkCallable;
import com.chatwala.android.networking.NetworkManager;
import com.chatwala.android.networking.requests.GetMessageReadUrl;
import com.chatwala.android.networking.requests.GetMessageReadUrlResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;


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
                //chatwalaMessage.initMetadata(activity, new JSONObject(IOUtils.toString(input)));
                chatwalaMessage.populateFromMetaDataJSON(new JSONObject(IOUtils.toString(input)));
                chatwalaMessage.setMessageMetaDataString(IOUtils.toString(input));


                input.close();

                return chatwalaMessage;
            }
            catch (FileNotFoundException e)
            {
                Logger.e("Couldn't extract file attachment for " + (uri == null ? "none" : uri.toString()), e);
                return null;
            }
            catch (IOException e)
            {
                Logger.e("Couldn't extract file attachment for " + (uri == null ? "none" : uri.toString()), e);
                throw new RuntimeException(e);
            }
            catch (JSONException e)
            {
                Logger.e("Couldn't extract file attachment for " + (uri == null ? "none" : uri.toString()), e);
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
            Logger.e("Failed extracting email from: " + (s == null ? "none" : s), e);
        }

        return null;
    }

    public static CWResult<GetMessageReadUrlResponse> getReadUrlFromShareUrl(Context context, Uri shareUrl) {

        String shareId = shareUrl.getQuery();

        GetMessageReadUrl getMessageReadUrl = new GetMessageReadUrl(shareId);

        NetworkCallable<HttpURLConnection, GetMessageReadUrlResponse> callable = getMessageReadUrl.getCallable(context, 3);

        Future<CWResult<GetMessageReadUrlResponse>> future = NetworkManager.getInstance().postToQueue(callable);

        try {
            return future.get();
        }
        catch (Exception e) {
            return null;
        }
    }

    /*
    public static String getMessageIdFromShareUrl(Uri shareUrl) {
        try {
            String[] split = shareUrl.getQuery().split("\\.");
            String messageId="";

            if(split.length==1) {
                //handle old share urls:
                messageId = split[0];
            }
            else if(split.length==2){
                messageId= split[1];
            }
            return messageId;
        }
        catch(Exception e) {
            return null;
        }
    }
=======
        return messageId;

    }*/

    public static String getIdFromIntent(Intent callingIntent)
    {
        String uri = callingIntent.getDataString();
        Logger.setShareLink(uri);
        if(uri.startsWith(EnvironmentVariables.WEB_STRING))
            return uri.replace(EnvironmentVariables.WEB_STRING, "");
        else if(uri.startsWith(EnvironmentVariables.ALT_WEB_STRING))
            return uri.replace(EnvironmentVariables.ALT_WEB_STRING, "");
        else if(uri.startsWith(EnvironmentVariables.DEV_WEB_STRING))
            return uri.replace(EnvironmentVariables.DEV_WEB_STRING, "");
        else if(uri.startsWith(EnvironmentVariables.QA_WEB_STRING))
            return uri.replace(EnvironmentVariables.QA_WEB_STRING, "");
        else if(uri.startsWith(EnvironmentVariables.REDIRECT_STRING))
            return uri.replace(EnvironmentVariables.REDIRECT_STRING, "");
        else if(uri.startsWith(MARKET_STRING))
            return uri.replace(MARKET_STRING, "");
        else if(uri.startsWith(EnvironmentVariables.HASH_STRING))
            return uri.replace(EnvironmentVariables.HASH_STRING, "");
        else if(uri.startsWith(EnvironmentVariables.ALT_HASH_STRING))
            return uri.replace(EnvironmentVariables.ALT_HASH_STRING, "");
        else
            throw new RuntimeException("Invalid message id from intent");
    }
}
