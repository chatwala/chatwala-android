package co.touchlab.customcamera.util;

import org.apache.commons.io.IOUtils;

import java.io.*;
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
    public static void zipFiles(File outFile, List<File> filesIn) throws IOException
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
        ZipInputStream zipInput = null;

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
}