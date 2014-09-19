package com.chatwala.android.queue.jobs;

import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.RenewWriteUrlForMessageThumbRequest;
import com.chatwala.android.http.requests.UploadMessageThumbnailRequest;
import com.chatwala.android.messages.ChatwalaMessageBase;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.path.android.jobqueue.JobManager;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 11:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadMessageThumbJob extends CwJob {
    private ChatwalaMessageBase message;

    public static CwJob post(ChatwalaMessageBase message) {
        return new UploadMessageThumbJob(message).postMeToQueue();
    }

    private UploadMessageThumbJob() {}

    private UploadMessageThumbJob(ChatwalaMessageBase message) {
        super(new CwJobParams(Priority.UPLOAD_HIGH_PRIORITY).requireNetwork().persist());
        this.message = message;
    }

    @Override
    public String getUID() {
        return message.getMessageId();
    }

    @Override
    public void onRun() throws Throwable {
        RenewWriteUrlForMessageThumbRequest renewRequest = new RenewWriteUrlForMessageThumbRequest(message);
        renewRequest.log();
        CwHttpResponse<JSONObject> renewResponse = HttpClient.getJSONObject(renewRequest);
        NetworkLogger.log(renewResponse, renewResponse.getData().toString());
        if(renewResponse.getResponseCode() != 200) {
            throw new RuntimeException("Didn't get a 200 from RenewWriteUrlForMessageThumbRequest");
        }
        String writeUrl = renewResponse.getData().getString("write_url");

        UploadMessageThumbnailRequest putRequest = new UploadMessageThumbnailRequest(writeUrl, message);
        putRequest.log();
        CwHttpResponse<Void> putResponse = HttpClient.getResponse(putRequest, HttpClient.DEFAULT_FILE_TIMEOUT);
        NetworkLogger.log(putResponse, null);
        if(putResponse.getResponseCode() != 201) {
            throw new RuntimeException("Didn't get a 201 from UploadMessageThumbnailRequest");
        }
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getUploadQueue();
    }
}
