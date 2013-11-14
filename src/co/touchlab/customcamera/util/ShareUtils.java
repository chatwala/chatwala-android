package co.touchlab.customcamera.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/9/13
 * Time: 6:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShareUtils
{
    public static void shareEmail(final Activity activity, final File shareWala)
    {
        new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object... params)
            {
                File buildDir = new File(Environment.getExternalStorageDirectory(), "chat_" + System.currentTimeMillis());
                buildDir.mkdirs();

                File walaFile = new File(buildDir, "video.mp4");


                File outZip = null;
                try
                {

                    FileOutputStream output = new FileOutputStream(walaFile);
                    FileInputStream input = new FileInputStream(shareWala);
                    IOUtils.copy(input, output);

                    input.close();
                    output.close();

                    File metadataFile = new File(buildDir, "metadata.json");


                    FileWriter fileWriter = new FileWriter(metadataFile);
                    fileWriter.append("{\n" +
                            "  \"thread_index\" : 0,\n" +
                            "  \"thread_id\" : \"8C4FEF3E-D508-481A-8924-E1ADEED5C09E\",\n" +
                            "  \"message_id\" : \"130EAC33-AE78-47F2-84F4-4CA6770EE971\",\n" +
                            "  \"sender_id\" : null,\n" +
                            "  \"version_id\" : \"1.0\",\n" +
                            "  \"recipient_id\" : null,\n" +
                            "  \"timestamp\" : \"2013-11-08T13:56:08Z\",\n" +
                            "  \"start_recording\" : 2.84\n" +
                            "}");

                    fileWriter.close();

                    outZip = new File(Environment.getExternalStorageDirectory(), "chat.wala");

                    ZipUtil.zipFiles(outZip, Arrays.asList(buildDir.listFiles()));
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }

                return outZip;
            }

            @Override
            protected void onPostExecute(Object o)
            {
                File outZip = (File) o;
                Intent intent = new Intent(Intent.ACTION_SEND);

//                intent.setType("message/rfc822");

                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"kevin@touchlab.co"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "test reply");
                intent.putExtra(Intent.EXTRA_TEXT, "the video");

                intent.setType("application/wala");

                Uri uri = Uri.parse("file://"+ outZip);
                intent.putExtra(Intent.EXTRA_STREAM, uri);

                activity.startActivity(Intent.createChooser(intent, "Send email..."));
            }
        }.execute();
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
