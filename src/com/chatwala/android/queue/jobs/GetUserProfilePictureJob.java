package com.chatwala.android.queue.jobs;

import com.chatwala.android.files.FileManager;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.GetUserImageRequest;
import com.chatwala.android.http.requests.GetUserProfilePicReadUrlRequest;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.chatwala.android.users.UserManager;
import com.chatwala.android.util.BitmapUtils;
import com.chatwala.android.util.Logger;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.path.android.jobqueue.JobManager;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.Semaphore;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/27/2014
 * Time: 11:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserProfilePictureJob extends CwJob {
    private static final int CACHE_EXPIRE_MINUTES = 1;

    private String readUrl;
    private File localImage;
    private File tmpImageFile = null;
    private transient Throwable exceptionToThrow;

    public static CwJob post() {
        return new GetUserProfilePictureJob().postMeToQueue();
    }

    private GetUserProfilePictureJob() {
        super(new CwJobParams(Priority.DOWNLOAD_LOW_PRIORITY).requireNetwork());
        this.localImage = FileManager.getUserProfilePic();
        this.tmpImageFile = FileManager.getTempImageFile();
    }

    @Override
    public String getUID() {
        return "getUserProfilePic";
    }

    @Override
    public void onRun() throws Throwable {
        if(tmpImageFile != null && tmpImageFile.exists()) {
            handleDownloadedFile();
            return;
        }

        //prevents an infinite loop of 304s even though we don't have the resource
        if(!localImage.exists()) {
            UserManager.setUserProfilePicLastModified(null);
        }

        if(readUrl == null) {
            readUrl = UserManager.getUserProfilePicReadUrl();
            if(readUrl == null) {
                GetUserProfilePicReadUrlRequest request = new GetUserProfilePicReadUrlRequest();
                request.log();
                CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
                NetworkLogger.log(response, response.getData().toString());
                readUrl = response.getData().getString("profile_url");
                UserManager.setUserProfilePicReadUrl(readUrl);
            }
        }

        if(!localImage.exists() || System.currentTimeMillis() - localImage.lastModified() > 1000 * 60 * CACHE_EXPIRE_MINUTES) {
            final Semaphore waitForResponse = new Semaphore(1, true);
            waitForResponse.acquire();

            GetUserImageRequest request = new GetUserImageRequest(UserManager.getUserId(), readUrl, UserManager.getUserProfilePicLastModified());
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
                        //just let it go...most likely this is a new user who doesn't have a profile pic yet
                        waitForResponse.release();
                        return;
                    }

                    if (response.getResponseCode() == 304) {
                        if (tmpImageFile.exists()) {
                            tmpImageFile.delete();
                        }
                        if(localImage.exists()) {
                            localImage.setLastModified(System.currentTimeMillis());
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
                            UserManager.setUserProfilePicLastModified(response.getLastModified());
                        }
                        handleDownloadedFile();
                    } catch (Throwable innerE) {
                        Logger.e("There was an error handling the downloaded user's profile pic", innerE);
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
        //rotate the user thumb in case it's not a normal orientation
        BitmapUtils.rotateBitmap(tmpImageFile, localImage);
        tmpImageFile.delete();
        localImage.setLastModified(System.currentTimeMillis());
    }

    private void setExceptionToThrow(Throwable exceptionToThrow) {
        this.exceptionToThrow = exceptionToThrow;
    }

    @Override
    public int getRetryLimit() {
        return 3;
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getDownloadQueue();
    }
}
