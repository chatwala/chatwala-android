package com.chatwala.android.util;

import java.io.*;

public class IoUtils {
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static boolean copy(final InputStream input, final OutputStream output) {
        int n;
        try {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            while (EOF != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static void writeStreamToFile(InputStream is, File f) throws Exception {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(f));
            copy(is, bos);
        }
        finally {
            if(bos != null) {
                try {
                    bos.close();
                } catch(Exception ignore) {}
            }
        }
    }

    public static void writeFileToStream(File f, OutputStream os) throws Exception {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(f));
            copy(bis, os);
        }
        finally {
            if(bis != null) {
                try {
                    bis.close();
                } catch(Exception ignore) {}
            }
        }
    }
}
