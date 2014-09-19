package com.chatwala.android.util;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.StatFs;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 1:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class FileUtils {
    public static boolean move(final File from, final File to) throws Exception {
        if(!from.renameTo(to)) {
            boolean success = copy(from, to);
            if(success) {
                from.delete();
            }
            return success;
        }
        else {
            return true;
        }
    }

    public static boolean copy(final File from, final File to) throws Exception {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(from));
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(to));
        try {
            return IoUtils.copy(in, out);
        }
        finally {
            if(in != null) {
                try {
                    in.close();
                } catch(Exception ignore) {}
            }
            if(out != null) {
                try {
                    out.flush();
                }
                catch(Exception ignore) {}
                try {
                    out.close();
                } catch(Exception ignore) {}
            }
        }
    }

    public static void writeStringToFile(String data, File toFile) {
        FileWriter out = null;
        try {
            out = new FileWriter(toFile);
            out.write(data);
        }
        catch(Exception e) {
            Logger.e("Couldn't write to file", e);
        }
        finally {
            if(out != null) {
                try {
                    out.close();
                } catch (Exception ignore) {}
            }
        }
    }

    public static void writeBitmapToFile(Bitmap data, File toFile) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(toFile));
            data.compress(Bitmap.CompressFormat.PNG, 100, bos);
        }
        catch(Throwable e) {
            Logger.e("Couldn't write to file", e);
        }
        finally {
            if(bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch(Exception ignore) {}
            }
        }
    }

    public static void deleteContents(File deleteContentsOfDir) {
        File[] files = deleteContentsOfDir.listFiles();
        if(files != null) {
            for(File file : files) {
                deleteRecursive(file);
            }
        }
    }

    //this method is not suitable for typical use, but it works for us here
    //http://stackoverflow.com/a/779529/691639
    public static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File c : files) {
                    deleteRecursive(c);
                }
            }

            if (f.exists()) {
                f.delete();
            }
        }
        else {
            f.delete();
        }
    }

    //this method is not suitable for typical use (follows symlinks)
    public static long sizeOfDirectory(final File directory) {
        final File[] files = directory.listFiles();
        if(files == null) {
            return 0L;
        }
        long size = 0;

        for(final File file : files) {
            size += sizeOfFile(file);
            if(size < 0) {
                break;
            }
        }

        return size;
    }

    private static long sizeOfFile(File f) {
        if(f.isDirectory()) {
            return sizeOfDirectory(f);
        }
        else {
            return f.length();
        }
    }

    public static String toString(File file) {
        BufferedReader reader = null;
        try {
            StringBuilder result = new StringBuilder();

            reader = new BufferedReader(new FileReader(file));

            char[] buf = new char[1024];
            int count;

            while ((count = reader.read(buf)) != -1) {
                result.append(buf, 0, count);
            }

            return result.toString();
        }
        catch(Exception e) {
            return null;
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public static long getAvailable(File path) {
        StatFs stat = new StatFs(path.getPath());
        long blockSize = getBlockSize(stat);
        long availableBlocks = getAvailableBlocks(stat);
        return availableBlocks * blockSize;
    }

    public static long getTotal(File path) {
        StatFs stat = new StatFs(path.getPath());
        long blockSize = getBlockSize(stat);
        long blockCount = getBlockCount(stat);
        return blockCount * blockSize;
    }

    @SuppressWarnings("deprecated")
    private static long getBlockSize(StatFs stat) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return stat.getBlockSizeLong();
        }
        else {
            return (long) stat.getBlockSize();
        }
    }

    @SuppressWarnings("deprecated")
    private static long getAvailableBlocks(StatFs stat) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return stat.getAvailableBlocksLong();
        }
        else {
            return (long) stat.getAvailableBlocks();
        }
    }

    @SuppressWarnings("deprecated")
    private static long getBlockCount(StatFs stat) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return stat.getBlockCountLong();
        }
        else {
            return (long) stat.getBlockCount();
        }
    }
}
