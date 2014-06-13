package com.chatwala.android.queue.jobs;

import com.chatwala.android.events.DrawerUpdateEvent;
import com.chatwala.android.files.FileManager;
import com.chatwala.android.files.ImageManager;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.GetMessageImageRequest;
import com.chatwala.android.messages.ChatwalaMessageBase;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.chatwala.android.util.BitmapUtils;
import com.chatwala.android.util.Logger;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.path.android.jobqueue.JobManager;
import de.greenrobot.event.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.Semaphore;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/13/2014
 * Time: 5:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessageImageJob extends CwJob {
    private static final int CACHE_EXPIRE_MINUTES = 1;

    private ChatwalaMessageBase message;
    private File tmpImageFile = null;
    private transient Throwable exceptionToThrow;

    public static CwJob post(ChatwalaMessageBase message) {
        return new GetMessageImageJob(message).postMeToQueue();
    }

    private GetMessageImageJob() {}

    private GetMessageImageJob(ChatwalaMessageBase message) {
        super(new CwJobParams(Priority.DOWNLOAD_MID_PRIORITY).requireNetwork().persist());

        this.message = message;
        this.tmpImageFile = FileManager.getTempImageFile();
    }

    @Override
    public String getUID() {
        return message.getMessageId();
    }

    @Override
    public void onRun() throws Throwable {
        if(tmpImageFile != null && tmpImageFile.exists()) {
            handleDownloadedFile();
            return;
        }

        //prevents an infinite loop of 304s even though we don't have the resource
        if(!message.getLocalMessageImage().exists()) {
            message.setImageModifiedSince(null);
            message.getDao().update(message);
        }

        /*
        Only try to get the thumb from server if it doesn't exists locally
        or if it's been cached for more than CACHE_EXPIRE_MINUTES.
        This is separate from message.getImageModifiedSince which is a cache
        control for the server side
        */
        File image = message.getLocalMessageImage();
        if(!image.exists() || System.currentTimeMillis() - image.lastModified() > 1000 * 60 * CACHE_EXPIRE_MINUTES) {
            final Semaphore waitForResponse = new Semaphore(1, true);
            waitForResponse.acquire();

            GetMessageImageRequest request = new GetMessageImageRequest(message);
            request.log();
            HttpClient.requestFile(request, tmpImageFile.getAbsolutePath(), new AsyncHttpClient.FileCallback() {
                @Override
                public void onCompleted(Exception e, AsyncHttpResponse rawResponse, File file) {
                    if (tmpImageFile == null || !tmpImageFile.exists()) {
                        setExceptionToThrow(new RuntimeException("Didn't get downloaded file"));
                        waitForResponse.release();
                        return;
                    }

                    if(e != null) {
                        if (tmpImageFile.exists()) {
                            tmpImageFile.delete();
                        }
                        setExceptionToThrow(e);
                        waitForResponse.release();
                        return;
                    }

                    CwHttpResponse<Void> response = new CwHttpResponse<Void>(rawResponse);
                    NetworkLogger.log(response, null);
                    tmpImageFile = file;

                    if (response.getResponseCode() == 404) {
                        if (tmpImageFile.exists()) {
                            tmpImageFile.delete();
                        }
                        setExceptionToThrow(new FileNotFoundException("Got a 404 when downloading a message image"));
                        waitForResponse.release();
                        return;
                    }

                    if(response.getResponseCode() == 304) {
                        if (tmpImageFile.exists()) {
                            tmpImageFile.delete();
                        }
                        if(message.getLocalMessageImage().exists()) {
                            message.getLocalMessageImage().setLastModified(System.currentTimeMillis());
                        }
                        if(message.getLocalMessageThumb().exists()) {
                            message.getLocalMessageThumb().setLastModified(System.currentTimeMillis());
                        }
                        waitForResponse.release();
                        return;
                    }

                    if(tmpImageFile.length() == 0) {
                        if (tmpImageFile.exists()) {
                            tmpImageFile.delete();
                        }
                        setExceptionToThrow(new FileNotFoundException("Got an empty file"));
                        waitForResponse.release();
                        return;
                    }

                    try {
                        if(response.getLastModified() != null) {
                            message.setImageModifiedSince(response.getLastModified());
                        }
                        handleDownloadedFile();
                    } catch (Throwable innerE) {
                        Logger.e("There was an error handling the downloaded message image", innerE);
                        setExceptionToThrow(innerE);
                    } finally {
                        waitForResponse.release();
                    }
                }
            }, HttpClient.SHORTER_FILE_TIMEOUT);

            waitForResponse.acquire();
            if(exceptionToThrow != null) {
                throw exceptionToThrow;
            }
        }
    }

    private void handleDownloadedFile() throws Throwable {
        //rotate the image in case it's not a normal orientation
        BitmapUtils.rotateBitmap(tmpImageFile, message.getLocalMessageImage());
        tmpImageFile.delete();
        message.getLocalMessageImage().setLastModified(System.currentTimeMillis());
        ImageManager.createDrawerThumbFromFile(message.getLocalMessageImage(), message.getLocalMessageThumb());
        message.getDao().createOrUpdate(message);
        EventBus.getDefault().post(new DrawerUpdateEvent(DrawerUpdateEvent.REFRESH_EVENT_EXTRA));
    }

    private void setExceptionToThrow(Throwable exceptionToThrow) {
        this.exceptionToThrow = exceptionToThrow;
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getDownloadQueue();
    }
}
