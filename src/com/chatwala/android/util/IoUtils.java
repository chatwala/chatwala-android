package com.chatwala.android.util;

import java.io.*;

public class IoUtils {
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static boolean copy(final File from, final File to) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(from));
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(to));
        try {
            return copy(in, out);
        }
        finally {
            if(in != null) {
                in.close();
            }
            if(out != null) {
                out.flush();
                out.close();
            }
        }
    }

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
}
