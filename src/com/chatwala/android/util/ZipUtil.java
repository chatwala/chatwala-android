package com.chatwala.android.util;

import com.chatwala.android.AppPrefs;
import com.chatwala.android.NewCameraActivity;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.MessageMetadata;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/8/13
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class ZipUtil
{
    private static void zipFiles(File outFile, List<File> filesIn) throws IOException
    {
        final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFile));
        for (File file : filesIn)
        {
            FileInputStream inp = new FileInputStream(file);

            ZipEntry e = new ZipEntry(file.getName());
            out.putNextEntry(e);

            IOUtils.copy(inp, out);

            out.closeEntry();
            inp.close();
        }

        out.close();
    }

    public static void unzipFiles(File inZip, File outFolder) throws IOException
    {
        final ZipFile zipFile = new ZipFile(inZip);
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements())
        {
            final ZipEntry zipEntry = entries.nextElement();
            if (!zipEntry.isDirectory())
            {
                final String fileName = zipEntry.getName();
                InputStream dataIn = zipFile.getInputStream(zipEntry);
                File file = new File(outFolder, fileName);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                IOUtils.copy(dataIn, fileOutputStream);
                fileOutputStream.close();
                dataIn.close();
            }
        }
        zipFile.close();
    }

    public static File buildZipToSend(NewCameraActivity activity, final File videoFile, final ChatwalaMessage originalMessage, final VideoUtils.VideoMetadata originalVideoMetadata)
    {
        File buildDir = new File(MessageDataStore.getTempDirectory(activity.getApplication()), "chat_" + System.currentTimeMillis());
        buildDir.mkdirs();

        File walaFile = new File(buildDir, "video.mp4");

        File outZip;
        try
        {
            FileOutputStream output = new FileOutputStream(walaFile);
            FileInputStream input = new FileInputStream(videoFile);
            IOUtils.copy(input, output);

            input.close();
            output.close();

            File metadataFile = new File(buildDir, "metadata.json");

            MessageMetadata sendMessageMetadata = originalMessage != null ? originalMessage.copyOrMakeNewMetadata() : new MessageMetadata();
            sendMessageMetadata.incrementForNewMessage();

            long startRecordingMillis = Math.round(originalMessage != null ? originalMessage.getStartRecording() * 1000d : 0);
            long chatMessageDuration = originalVideoMetadata == null ? 0 : (originalVideoMetadata.duration + NewCameraActivity.VIDEO_PLAYBACK_START_DELAY);
            sendMessageMetadata.startRecording = ((double) Math.max(chatMessageDuration - startRecordingMillis, 0)) / 1000d;

            sendMessageMetadata.senderId = AppPrefs.getInstance(activity).getUserId();

            FileWriter fileWriter = new FileWriter(metadataFile);

            fileWriter.append(sendMessageMetadata.toJsonString());

            fileWriter.close();

            File shareDir = new File(MessageDataStore.getOutboxDirectory(activity.getApplication()), "sharefile_" + System.currentTimeMillis());
            shareDir.mkdirs();
            outZip = new File(shareDir, "chat.wala");

            zipFiles(outZip, Arrays.asList(buildDir.listFiles()));

            videoFile.delete();
            for(File file : buildDir.listFiles())
            {
                file.delete();
            }
            buildDir.delete();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }

        return outZip;
    }
}