package com.chatwala.android.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 12:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class ZipUtils {
    public static void unzipFiles(File zipFile, File toDirectory) throws Throwable {
        ZipFile zip = null;

        try {
            zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                if (!zipEntry.isDirectory()) {
                    String fileName = zipEntry.getName();
                    BufferedInputStream dataIn = null;
                    try {
                        dataIn = new BufferedInputStream(zip.getInputStream(zipEntry));
                        IoUtils.writeStreamToFile(dataIn, new File(toDirectory, fileName));
                    }
                    finally {
                        if(dataIn != null) {
                            dataIn.close();
                        }
                    }
                }
            }
        }
        finally {
            if(zip != null) {
                zip.close();
            }
        }
    }

    public static void zipFiles(File result, File... filesToZip) throws Throwable {
        zipFiles(result, Arrays.asList(filesToZip));
    }

    public static void zipFiles(File result, List<File> filesToZip) throws Throwable {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(result));
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(bos);
            for(File file : filesToZip) {
                ZipEntry entry = new ZipEntry(file.getName());
                zos.putNextEntry(entry);
                IoUtils.writeFileToStream(file, zos);
                zos.closeEntry();
            }
        }
        finally {
            if(zos != null) {
                try {
                    zos.close();
                } catch(Exception ignore) {}
            }
        }
    }
}
