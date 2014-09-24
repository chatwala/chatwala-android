package com.chatwala.android.queue.jobs;

import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.GetUserProfilePictureWriteUrlRequest;
import com.chatwala.android.http.requests.UploadUserProfilePicRequest;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.NetworkConnectionChecker;
import com.chatwala.android.queue.Priority;
import com.staticbloc.jobs.JobInitializer;
import com.staticbloc.jobs.JobQueue;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 7:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadUserProfilePicJob extends CwJob {
    public static CwJob post() {
        return new UploadUserProfilePicJob().postMeToQueue();
    }

    private UploadUserProfilePicJob() {
        super(new JobInitializer()
                .requiresNetwork(true)
                .isPersistent(true)
                .priority(Priority.UPLOAD_MID_PRIORITY));
    }

    @Override
    public String getUID() {
        return "uploadProfilePic";
    }

    @Override
    public void performJob() throws Throwable {
        GetUserProfilePictureWriteUrlRequest renewRequest = new GetUserProfilePictureWriteUrlRequest();
        renewRequest.log();
        CwHttpResponse<JSONObject> renewResponse = HttpClient.getJSONObject(renewRequest);
        NetworkLogger.log(renewResponse, renewResponse.getData().toString());
        if(renewResponse.getData().getJSONObject("response_code").getInt("code") != 1) {
            throw new RuntimeException("Didn't get a valid cw response code for the user profile pic write url");
        }
        String writeUrl = renewResponse.getData().getString("write_url");

        UploadUserProfilePicRequest putRequest = new UploadUserProfilePicRequest(writeUrl);
        putRequest.log();
        CwHttpResponse<Void> putResponse = HttpClient.getResponse(putRequest, HttpClient.DEFAULT_FILE_TIMEOUT);
        NetworkLogger.log(putResponse, null);
        if(putResponse.getResponseCode() != 201) {
            throw new RuntimeException("Didn't put the user profile pic successfully");
        }
    }

    @Override
    protected JobQueue getQueueToPostTo() {
        return getUploadQueue();
    }

    @Override
    public boolean canReachRequiredNetwork() {
        return NetworkConnectionChecker.getInstance().isConnected();
    }
}
