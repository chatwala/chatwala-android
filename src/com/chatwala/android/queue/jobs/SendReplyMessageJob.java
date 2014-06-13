package com.chatwala.android.queue.jobs;

import com.chatwala.android.camera.VideoMetadata;
import com.chatwala.android.db.DatabaseHelper;
import com.chatwala.android.events.DrawerUpdateEvent;
import com.chatwala.android.files.FileManager;
import com.chatwala.android.files.ImageManager;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.CompleteReplyMessageRequest;
import com.chatwala.android.http.requests.RenewWriteUrlForMessageRequest;
import com.chatwala.android.http.requests.StartReplyMessageRequest;
import com.chatwala.android.http.requests.UploadWalaRequest;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.ChatwalaSentMessage;
import com.chatwala.android.messages.MessageState;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.chatwala.android.util.FileUtils;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.VideoUtils;
import com.chatwala.android.util.ZipUtils;
import com.path.android.jobqueue.JobManager;
import de.greenrobot.event.EventBus;
import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/12/2014
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class SendReplyMessageJob extends CwJob {
    private ChatwalaMessage replyingToMessage;
    private ChatwalaSentMessage newMessage;
    private File recordedFile;

    private double newMessageStartRecording;
    private String replyWriteUrl;

    private boolean initted = false;
    private boolean startSucceeded = false;
    private boolean uploadUserThumbSucceeded = false;
    private boolean uploadMessageThumbSucceeded = false;
    private boolean putPreviouslyFailed = false;
    private boolean putSucceeded = false;
    private boolean finalizeSucceeded = false;
    private boolean markRepliedSucceeded = false;

    public static CwJob post(ChatwalaMessage replyingToMessage, File recordedFile) {
        return new SendReplyMessageJob(replyingToMessage, recordedFile).postMeToQueue();
    }

    private SendReplyMessageJob() {}

    private SendReplyMessageJob(ChatwalaMessage replyingToMessage, File recordedFile) {
        super(new CwJobParams(Priority.UPLOAD_IMMEDIATE_PRIORITY).requireNetwork().persist());

        this.replyingToMessage = replyingToMessage;
        this.recordedFile = recordedFile;

        newMessage = new ChatwalaSentMessage(UUID.randomUUID().toString());
    }

    @Override
    public String getUID() {
        return replyingToMessage.getMessageId() + newMessage.getMessageId();
    }

    @Override
    public void onRun() throws Throwable {
        if(!initted) {
            if(!recordedFile.exists()) {
                //TODO how'd this happen? Who do we alert about this?
                Logger.w("Recorded file doesn't exist?");
                return;
            }

            File outboxVideoFile = newMessage.getOutboxVideoFile();
            FileUtils.move(recordedFile, outboxVideoFile);
            recordedFile = outboxVideoFile;
            VideoMetadata metadata = VideoUtils.parseVideoMetadata(replyingToMessage.getLocalVideoFile());
            double replyingToStartRecording = replyingToMessage.getStartRecording() * 1000d;
            double replyingToVideoDuration = metadata.getDuration();
            newMessageStartRecording = (replyingToVideoDuration - replyingToStartRecording) / 1000d;
            initted = true;
        }

        if(!startSucceeded) {
            StartReplyMessageRequest request = new StartReplyMessageRequest(newMessage, replyingToMessage, newMessageStartRecording);
            request.log();
            CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
            NetworkLogger.log(response, response.getData().toString());
            newMessage.populateFromMetadata(response.getData().getJSONObject("message_meta_data"));
            FileUtils.writeStringToFile(newMessage.getMessageMetadataString(), FileManager.getMetadataFromOutboxMessageDir(newMessage));
            replyWriteUrl = response.getData().getString("write_url");
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
            RenewWriteUrlForMessageRequest renewRequest = new RenewWriteUrlForMessageRequest(newMessage);
            renewRequest.log();
            CwHttpResponse<JSONObject> renewResponse = HttpClient.getJSONObject(renewRequest);
            NetworkLogger.log(renewResponse, renewResponse.getData().toString());
            if(renewResponse.getResponseCode() != 200) {
                throw new RuntimeException("RenewWriteUrlForMessageRequest didn't return a 200...try again");
            }
            replyWriteUrl = renewResponse.getData().getString("write_url");
        }
        if(!putSucceeded) {
            if(!recordedFile.exists()) {
                //TODO how'd this happen? Who do we alert about this?
                Logger.w("Recorded file doesn't exist?");
                return;
            }

            File wala = newMessage.getOutboxWalaFile();
            File metadata = newMessage.getOutboxMetadataFile();
            try {
                ZipUtils.zipFiles(wala, recordedFile, metadata);

                UploadWalaRequest uploadWalaRequest = new UploadWalaRequest(replyWriteUrl, wala);
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
            FileUtils.move(recordedFile, newMessage.getLocalVideoFile());
            FileUtils.move(metadata, newMessage.getLocalMetadataFile());
        }

        if(!finalizeSucceeded) {
            CompleteReplyMessageRequest finalizeRequest = new CompleteReplyMessageRequest(newMessage);
            finalizeRequest.log();
            CwHttpResponse<JSONObject> finalizeResponse = HttpClient.getJSONObject(finalizeRequest);
            NetworkLogger.log(finalizeResponse, finalizeResponse.getData().toString());
            if(finalizeResponse.getResponseCode() != 200) {
                throw new RuntimeException("CompleteReplyMessageRequest didn't return a 200...try again");
            }

            JSONObject responseCode = finalizeResponse.getData().getJSONObject("response_code");
            if(responseCode.getInt("code") != 1) {
                throw new RuntimeException("Got a bad response code from CompleteReplyMessageRequest");
            }

            DatabaseHelper.get().getChatwalaSentMessageDao().createOrUpdate(newMessage);

            finalizeSucceeded = true;
        }

        if(!uploadMessageThumbSucceeded) {
            ImageManager.createMessageThumbFromVideoFrame(newMessage.getLocalVideoFile(), newMessage);
            UploadMessageThumbJob.post(newMessage);
            uploadMessageThumbSucceeded = true;
        }

        if(!markRepliedSucceeded) {
            replyingToMessage.setMessageState(MessageState.REPLIED);
            DatabaseHelper.get().getChatwalaMessageDao().createOrUpdate(replyingToMessage);
            EventBus.getDefault().post(new DrawerUpdateEvent(DrawerUpdateEvent.LOAD_EVENT_EXTRA));
            markRepliedSucceeded = true;
        }
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getUploadQueue();
    }
}
