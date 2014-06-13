package com.chatwala.android.queue.jobs;

import com.chatwala.android.db.DatabaseHelper;
import com.chatwala.android.files.FileManager;
import com.chatwala.android.files.ImageManager;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.CompleteUnknownRecipientMessageRequest;
import com.chatwala.android.http.requests.RenewWriteUrlForMessageRequest;
import com.chatwala.android.http.requests.StartUnknownRecipientMessageRequest;
import com.chatwala.android.http.requests.UploadWalaRequest;
import com.chatwala.android.messages.ChatwalaSentMessage;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.chatwala.android.util.FileUtils;
import com.chatwala.android.util.ZipUtils;
import com.path.android.jobqueue.JobManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 6:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class SendUnknownRecipientMessageJob extends CwJob {
    private File recordedFile;
    private String messageId;

    private boolean moveToOutboxFinished = false;
    private boolean startSucceeded = false;
    private boolean uploadUserThumbSucceeded = false;
    private boolean uploadMessageThumbSucceeded = false;
    private boolean putPreviouslyFailed = false;
    private boolean putSucceeded = false;
    private boolean finalizeSucceeded = false;

    private ChatwalaSentMessage message = null;
    private String messageWriteUrl = null;

    private static class StartResponse {
        private JSONObject json;

        public StartResponse(JSONObject json) throws JSONException {
            if(json == null) {
                throw new JSONException("StartUnknownRecipientMessage response cannot be null");
            }
            this.json = json;
        }

        public JSONObject getMetadata() throws JSONException {
            return json.getJSONObject("message_meta_data");
        }

        public int getCwResponseCode() throws JSONException {
            return json.getJSONObject("response_code").getInt("code");
        }

        public String getCwResponseMessage() throws JSONException {
            return json.getJSONObject("response_code").getString("message");
        }

        public String getMessageWriteUrl() throws JSONException {
            return json.getString("write_url");
        }

        public String getMessageThumbnailWriteUrl() throws JSONException {
            return json.getString("message_thumbnail_write_url");
        }
    }

    public static CwJob post(File recordedFile, String messageId) {
        return new SendUnknownRecipientMessageJob(recordedFile, messageId).postMeToQueue();
    }

    private SendUnknownRecipientMessageJob() {}

    private SendUnknownRecipientMessageJob(File recordedFile, String messageId) {
        super(new CwJobParams(Priority.UPLOAD_IMMEDIATE_PRIORITY).requireNetwork().persist());

        this.recordedFile = recordedFile;
        this.messageId = messageId;
        this.message = new ChatwalaSentMessage(this.messageId);
    }

    @Override
    public String getUID() {
        return messageId;
    }

    @Override
    public void onRun() throws Throwable {
        if(message == null) {
            message = new ChatwalaSentMessage(messageId);
        }

        if(!moveToOutboxFinished) {
            File outboxVideoFile = message.getOutboxVideoFile();
            FileUtils.move(recordedFile, outboxVideoFile);
            recordedFile = outboxVideoFile;
            moveToOutboxFinished = true;
        }

        if(!startSucceeded) {
            StartUnknownRecipientMessageRequest startRequest = new StartUnknownRecipientMessageRequest(messageId);
            startRequest.log();
            CwHttpResponse<JSONObject> startResponse = HttpClient.getJSONObject(startRequest);
            NetworkLogger.log(startResponse, startResponse.getData().toString());
            if(startResponse.getResponseCode() != 200) {
                throw new RuntimeException("StartUnknownRecipientMessageRequest didn't return a 200...try again");
            }
            StartResponse response = new StartResponse(startResponse.getData());
            message.populateFromMetadata(response.getMetadata());
            FileUtils.writeStringToFile(message.getMessageMetadataString(), FileManager.getMetadataFromOutboxMessageDir(message));
            messageWriteUrl = response.getMessageWriteUrl();
            startSucceeded = true;
        }

        if(!uploadUserThumbSucceeded) {
            //create the user's profile pic if one doesn't exist
            if(!ImageManager.profilePicExists()) {
                ImageManager.createProfilePicFromVideoFrame(recordedFile);
                UploadUserProfilePicJob.post();
            }
            uploadUserThumbSucceeded = true;
        }

        if(putPreviouslyFailed) {
            RenewWriteUrlForMessageRequest renewRequest = new RenewWriteUrlForMessageRequest(message);
            renewRequest.log();
            CwHttpResponse<JSONObject> renewResponse = HttpClient.getJSONObject(renewRequest);
            NetworkLogger.log(renewResponse, renewResponse.getData().toString());
            if(renewResponse.getResponseCode() != 200) {
                throw new RuntimeException("RenewWriteUrlForMessageRequest didn't return a 200...try again");
            }
            messageWriteUrl = renewResponse.getData().getString("write_url");
        }
        if(!putSucceeded) {
            if(!recordedFile.exists()) {
                //TODO how'd this happen? Who do we alert about this?
                return;
            }

            File wala = message.getOutboxWalaFile();
            File metadata = message.getOutboxMetadataFile();
            try {
                ZipUtils.zipFiles(wala, recordedFile, metadata);

                UploadWalaRequest uploadWalaRequest = new UploadWalaRequest(messageWriteUrl, wala);
                uploadWalaRequest.log();
                CwHttpResponse<Void> uploadWalaResponse = HttpClient.getResponse(uploadWalaRequest, HttpClient.DEFAULT_FILE_TIMEOUT);
                NetworkLogger.log(uploadWalaResponse, null);
                if(uploadWalaResponse.getResponseCode() != 201) {
                    throw new RuntimeException("UploadWalaRequest didn't return a 201...try again");
                }
            }
            catch(Throwable e) {
                putPreviouslyFailed = true;
                throw e;
            }
            putSucceeded = true;
            putPreviouslyFailed = false;
            FileUtils.move(recordedFile, message.getLocalVideoFile());
            FileUtils.move(metadata, message.getLocalMetadataFile());
        }

        if(!finalizeSucceeded) {
            CompleteUnknownRecipientMessageRequest finalizeRequest = new CompleteUnknownRecipientMessageRequest(message);
            finalizeRequest.log();
            CwHttpResponse<JSONObject> finalizeResponse = HttpClient.getJSONObject(finalizeRequest);
            NetworkLogger.log(finalizeResponse, finalizeResponse.getData().toString());
            if(finalizeResponse.getResponseCode() != 200) {
                throw new RuntimeException("CompleteUnknownRecipientMessageRequest didn't return a 200...try again");
            }

            JSONObject returnedMetadata = finalizeResponse.getData().getJSONObject("message_meta_data");
            message.populateFromMetadata(returnedMetadata);

            DatabaseHelper.get().getChatwalaSentMessageDao().createOrUpdate(message);

            finalizeSucceeded = true;
        }

        if(!uploadMessageThumbSucceeded) {
            ImageManager.createMessageThumbFromVideoFrame(message.getLocalVideoFile(), message);
            UploadMessageThumbJob.post(message);
            uploadMessageThumbSucceeded = true;
        }
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getUploadQueue();
    }
}
