package com.chatwala.android.util;

import android.content.Context;
import android.util.Log;
import com.chatwala.android.ChatwalaApplication;
import com.crashlytics.android.Crashlytics;
import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {
    private static String TAG = "Logger";
    private static boolean LOG_TO_ANDROID_DEFAULT = false;
    private static int LOG_MAX_SIZE = 1024 * 512;
    private static ChatwalaApplication app;

    public static final String CL_USER_ACTION = "USER_ACTION";
    public static final String CL_MEDIA_RECORDER_STATE = "MEDIA_RECORDER_STATE";

    private static final String CL_PREVIEW_WIDTH = "PREVIEW_WIDTH";
    private static final String CL_PREVIEW_HEIGHT = "PREVIEW_HEIGHT";
    private static final String CL_VIDEO_WIDTH = "VIDEO_WIDTH";
    private static final String CL_VIDEO_HEIGHT = "VIDEO_HEIGHT";
    private static final String CL_FRAMERATE = "FRAMERATE";
    private static final String CL_SHARE_LINK = "SHARE_LINK";
    private static final String CL_USING_SMS = "USING_SMS";
    private static final String CL_REFRESH_INTERVAL = "REFRESH_INTERVAL";
    private static final String CL_STORAGE_LIMIT = "STORAGE_LIMIT";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Logger() {}

    public static void init(ChatwalaApplication app) {
        Logger.app = app;
    }

    public static void init(ChatwalaApplication app, String tag) {
        Logger.app = app;
        TAG = tag;
    }

    public static void init(ChatwalaApplication app, String tag, boolean logToAndroidDefault) {
        Logger.app = app;
        TAG = tag;
        LOG_TO_ANDROID_DEFAULT = logToAndroidDefault;
    }

    public static void init(ChatwalaApplication app, String tag, int maxLogFileSizeInBytes) {
        Logger.app = app;
        TAG = tag;
        LOG_MAX_SIZE = maxLogFileSizeInBytes;
    }

    public static void init(ChatwalaApplication app, String tag, boolean logToAndroidDefault, int maxLogFileSizeInBytes) {
        Logger.app = app;
        TAG = tag;
        LOG_TO_ANDROID_DEFAULT = logToAndroidDefault;
        LOG_MAX_SIZE = maxLogFileSizeInBytes;
    }

    private static final class LogTask implements Runnable {
        private String message;
        private Throwable t;
        private int level;
        private StackTraceElement ste;
        private boolean logToAndroid;

        public LogTask(String message, Throwable t, int level, StackTraceElement ste, boolean logToAndroid) {
            this.message = message;
            this.t = t;
            this.level = level;
            this.ste = ste;
            this.logToAndroid = logToAndroid;
        }

        @Override
        public void run() {
            if(logToAndroid) {
                logToAndroid(createAndroidMessage(message), t, level);
            }

            String message = createMessage();
            if(message == null) {
                return;
            }

            if(app != null) {
                try {
                    File log = new File(app.getFilesDir(), "log");
                    File afterCrash = new File(app.getFilesDir(), "afterCrash");
                    if(afterCrash.exists()) {
                        afterCrash.delete();
                        Log.e(TAG, "We got a crash");
                    }
                    if(log.exists()) {
                        if(log.length() > LOG_MAX_SIZE) { //512KB
                            log.delete();
                        }
                    }

                    PrintWriter pw = new PrintWriter(app.openFileOutput("log", Context.MODE_APPEND));
                    pw.println(message);
                    pw.close();
                }
                catch(Exception e) {
                    Log.e(TAG, "Got an error in LogTask", e);
                }
            }
            else {
                Log.e(TAG, "Couldn't get a context in LogTask");
            }
        }

        private String createMessage() {
            String[] classNames = ste.getClassName().split("\\.");
            String sourceMethod = (classNames.length > 0 ? classNames[classNames.length - 1] : ste.getClassName()) + "." +
                    ste.getMethodName() + "()";

            if(t != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                message += "\n" + sw.toString();
                pw.close();
                Crashlytics.logException(t);
            }

            try {
                JSONObject json = new JSONObject();
                json.put("LEVEL", printLogLevel(level))
                        .put("SOURCE_FILE", ste.getFileName())
                        .put("SOURCE_LINE", ste.getLineNumber())
                        .put("SOURCE_METHOD", sourceMethod)
                        .put("MESSAGE", message)
                        .put("TIMESTAMP", new Date().getTime());
                return json.toString();

            }
            catch(Exception e) {
                Log.e(TAG, "Could not build the message in Logger.onHandleIntent()", e);
                return null;
            }
        }

        private String createAndroidMessage(String message) {
            String[] classNames = ste.getClassName().split("\\.");
            return ste.getFileName() + ":" +
                    ste.getLineNumber() + " - " +
                    (classNames.length > 0 ? classNames[classNames.length - 1] : ste.getClassName()) + "." +
                    ste.getMethodName() + "() - " +
                    message;
        }
    }

    private static void log(String message, Throwable t, int level, boolean logToAndroid) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
        executor.execute(new LogTask(message, t, level, ste, logToAndroid));
    }

    public static void setTag(String tag) {
        TAG = tag;
    }

    public static void setLogToAndroidDefault(boolean logToAndroidDefault) {
        LOG_TO_ANDROID_DEFAULT = logToAndroidDefault;
    }

    public static void d() {
        log("", null, Log.DEBUG, LOG_TO_ANDROID_DEFAULT);
    }

    public static void d(String message) {
        log(message, null, Log.DEBUG, LOG_TO_ANDROID_DEFAULT);
    }

    public static void d(String message, Throwable t) {
        log(message, t, Log.DEBUG, LOG_TO_ANDROID_DEFAULT);
    }

    public static void d(String message, boolean logToAndroid) {
        log(message, null, Log.DEBUG, logToAndroid);
    }

    public static void d(String message, Throwable t, boolean logToAndroid) {
        log(message, t, Log.DEBUG, logToAndroid);
    }

    public static void e() {
        log("", null, Log.ERROR, LOG_TO_ANDROID_DEFAULT);
    }

    public static void e(String message) {
        log(message, null, Log.ERROR, LOG_TO_ANDROID_DEFAULT);
    }

    public static void e(String message, Throwable t) {
        log(message, t, Log.ERROR, LOG_TO_ANDROID_DEFAULT);
    }

    public static void e(String message, boolean logToAndroid) {
        log(message, null, Log.ERROR, logToAndroid);
    }

    public static void e(String message, Throwable t, boolean logToAndroid) {
        log(message, t, Log.ERROR, logToAndroid);
    }

    public static void i() {
        log("", null, Log.INFO, LOG_TO_ANDROID_DEFAULT);
    }

    public static void i(String message) {
        log(message, null, Log.INFO, LOG_TO_ANDROID_DEFAULT);
    }

    public static void i(String message, Throwable t) {
        log(message, t, Log.INFO, LOG_TO_ANDROID_DEFAULT);
    }

    public static void i(String message, boolean logToAndroid) {
        log(message, null, Log.INFO, logToAndroid);
    }

    public static void i(String message, Throwable t, boolean logToAndroid) {
        log(message, t, Log.INFO, logToAndroid);
    }

    public static void v() {
        log("", null, Log.VERBOSE, LOG_TO_ANDROID_DEFAULT);
    }

    public static void v(String message) {
        log(message, null, Log.VERBOSE, LOG_TO_ANDROID_DEFAULT);
    }

    public static void v(String message, Throwable t) {
        log(message, t, Log.VERBOSE, LOG_TO_ANDROID_DEFAULT);
    }

    public static void v(String message, boolean logToAndroid) {
        log(message, null, Log.VERBOSE, logToAndroid);
    }

    public static void v(String message, Throwable t, boolean logToAndroid) {
        log(message, t, Log.VERBOSE, logToAndroid);
    }

    public static void w() {
        log("", null, Log.WARN, LOG_TO_ANDROID_DEFAULT);
    }

    public static void w(String message) {
        log(message, null, Log.WARN, LOG_TO_ANDROID_DEFAULT);
    }

    public static void w(String message, Throwable t) {
        log(message, t, Log.WARN, LOG_TO_ANDROID_DEFAULT);
    }

    public static void w(String message, boolean logToAndroid) {
        log(message, null, Log.WARN, logToAndroid);
    }

    public static void w(String message, Throwable t, boolean logToAndroid) {
        log(message, t, Log.WARN, logToAndroid);
    }

    public static void crashlytics(String message) {
        log(message, null, Log.INFO, LOG_TO_ANDROID_DEFAULT);
    }

    public static void crashlytics(String message, boolean logToAndroid) {
        log(message, null, Log.INFO, logToAndroid);
    }

    public static void crashlytics(String message, Throwable t) {
        log(message, t, Log.ERROR, LOG_TO_ANDROID_DEFAULT);
    }

    public static void crashlytics(String message, Throwable t, boolean logToAndroid) {
        log(message, t, Log.ERROR, logToAndroid);
    }

    public static void logUserAction(String message) {
        log(message, null, Log.DEBUG, LOG_TO_ANDROID_DEFAULT);
        Crashlytics.log(Log.DEBUG, CL_USER_ACTION, message);
    }

    public static void logMediaRecorderState(String mediaRecorderState) {
        log(mediaRecorderState, null, Log.DEBUG, LOG_TO_ANDROID_DEFAULT);
        Crashlytics.log(Log.DEBUG, CL_MEDIA_RECORDER_STATE, mediaRecorderState);
    }

    public static void setUserInfo(String userIdentifier, String userName, String userEmail) {
        Crashlytics.setUserIdentifier(userIdentifier);
        Crashlytics.setUserName(userName);
        Crashlytics.setUserEmail(userEmail);
    }

    public static void logPreviewDimensions(int width, int height) {
        Crashlytics.setInt(CL_PREVIEW_WIDTH, width);
        Crashlytics.setInt(CL_PREVIEW_HEIGHT, height);
    }

    public static void logVideoDimensions(int width, int height) {
        Crashlytics.setInt(CL_VIDEO_WIDTH, width);
        Crashlytics.setInt(CL_VIDEO_HEIGHT, height);
    }

    public static void logFramerate(int framerate) {
        Crashlytics.setInt(CL_FRAMERATE, framerate);
    }

    public static void logShareLink(String link) {
        Crashlytics.setString(CL_SHARE_LINK, link);
    }

    public static void logUsingSms(Boolean usingSms) {
        Crashlytics.setBool(CL_USING_SMS, usingSms);
    }

    public static void logRefreshInterval(int refreshInterval) {
        Crashlytics.setInt(CL_REFRESH_INTERVAL, refreshInterval);
    }

    public static void logStorageLimit(int storageLimit) {
        Crashlytics.setInt(CL_STORAGE_LIMIT, storageLimit);
    }

    private static void logToAndroid(String message, Throwable t, int level) {
        switch(level) {
            case Log.ASSERT:
                System.out.println(message);
                break;
            case Log.DEBUG:
                if(t == null) {
                    Log.d(TAG, message);
                }
                else {
                    Log.d(TAG, message, t);
                }
                break;
            case Log.ERROR:
                if(t == null) {
                    Log.e(TAG, message);
                }
                else {
                    Log.e(TAG, message, t);
                }
                break;
            case Log.INFO:
                if(t == null) {
                    Log.i(TAG, message);
                }
                else {
                    Log.i(TAG, message, t);
                }
                break;
            case Log.VERBOSE:
                if(t == null) {
                    Log.v(TAG, message);
                }
                else {
                    Log.v(TAG, message, t);
                }
                break;
            case Log.WARN:
                if(t == null) {
                    Log.w(TAG, message);
                }
                else {
                    Log.w(TAG, message, t);
                }
                break;
        }
    }

    private static String printLogLevel(int level) {
        switch(level) {
            case Log.ASSERT:
                return "ASSERT";
            case Log.DEBUG:
                return "DEBUG";
            case Log.ERROR:
                return "ERROR";
            case Log.INFO:
                return "INFO";
            case Log.VERBOSE:
                return "VERBOSE";
            case Log.WARN:
                return "WARN";
            default:
                return "OTHER";
        }
    }
}
