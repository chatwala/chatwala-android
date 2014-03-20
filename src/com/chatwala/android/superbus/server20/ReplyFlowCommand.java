package com.chatwala.android.superbus.server20;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.http.BaseSasPutRequest;
import com.chatwala.android.http.server20.ChatwalaResponse;
import com.chatwala.android.http.server20.RenewWriteUrlForMessageRequest;
import com.chatwala.android.http.server20.StartReplyMessageRequest;
import com.chatwala.android.http.server20.CompleteReplyMessageRequest;
import com.chatwala.android.loaders.BroadcastSender;
import com.chatwala.android.superbus.PutUserProfilePictureCommand;
import com.chatwala.android.util.ThumbUtils;
import com.j256.ormlite.dao.Dao;
import com.chatwala.android.util.VideoUtils;
import com.chatwala.android.util.ZipUtil;
import com.chatwala.android.activity.NewCameraActivity;
import com.chatwala.android.util.MessageDataStore;
import java.io.File;
import java.sql.SQLException;

import com.chatwala.android.AppPrefs;
import com.chatwala.android.dataops.DataProcessor;
import co.touchlab.android.superbus.BusHelper;


/**
 * Created by samirahman on 3/13/14.
 */
public class ReplyFlowCommand extends SqliteCommand {

    ChatwalaMessage replyingToMessage;
    String newMessageId;
    String videoFilePath;
    long videoDuration;
    String writeUrl;
    String messageMetaDataJSONString;
    String shardKey;

    boolean startCallSucceeded=false;
    boolean thumbSucceeded=false;
    boolean putCallSucceeded=false;
    boolean finalizeSucceeded=false;
    boolean putCallFailedPreviously = false;
    boolean markReadSucceeded=false;


    public ReplyFlowCommand() {}

    public ReplyFlowCommand(ChatwalaMessage replyingToMessage, String newMessageId, String videoFilePath, long videoDuration){
        this.replyingToMessage = replyingToMessage;
        this.videoFilePath = videoFilePath;
        this.videoDuration = videoDuration;
        this.newMessageId=newMessageId;
    }


    @Override
    public String logSummary() {
        return null;
    }

    @Override
    public boolean same(Command command) {
        if(command instanceof ReplyFlowCommand)
        {
            return this.newMessageId.equals(((ReplyFlowCommand) command).newMessageId);
        }

        return false;
    }


    @Override
    public void callCommand(Context context) throws TransientException, PermanentException {

        if(!this.startCallSucceeded) {
            //calculate start recording
            double startRecordingMillis = replyingToMessage.getStartRecording() * 1000d;
            double chatMessageDuration = videoDuration + NewCameraActivity.VIDEO_PLAYBACK_START_DELAY;
            double startRecording = Math.max(chatMessageDuration - startRecordingMillis, 0d) / 1000d;

            //start
            ChatwalaResponse<ChatwalaMessage> startResponse = (ChatwalaResponse<ChatwalaMessage>) new StartReplyMessageRequest(context, newMessageId, replyingToMessage.getMessageId(), startRecording).execute();
            ChatwalaMessage chatwalaMessage = startResponse.getResponseData();
            shardKey =chatwalaMessage.getShardKey();
            writeUrl = chatwalaMessage.getWriteUrl();
            messageMetaDataJSONString = chatwalaMessage.getMessageMetaDataString();

            if(startResponse.getResponseCode()!=1) {
                throw new TransientException();
            }

            this.startCallSucceeded=true;
        }

        if(!thumbSucceeded) {
            //create first profile picture if needed
            if(!MessageDataStore.findUserImageInLocalStore(AppPrefs.getInstance(context).getUserId()).exists())
            {
                final File thumbFile = ThumbUtils.createThumbFromFirstFrame(context, videoFilePath);
                final Context applicationContext = context.getApplicationContext();

                DataProcessor.runProcess(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        BusHelper.submitCommandSync(applicationContext, new UploadUserProfilePictureCommand(thumbFile.getPath()));
                    }
                });

            }

            this.thumbSucceeded=true;
        }


        if(putCallFailedPreviously) {
            ChatwalaResponse<String> renewResponse = (ChatwalaResponse<String>) new RenewWriteUrlForMessageRequest(context, newMessageId, shardKey).execute();
            writeUrl = renewResponse.getResponseData();
        }

        if(!putCallSucceeded) {
            //create wala file
            File outZip = ZipUtil.buildZipToSend(context, new File(videoFilePath), messageMetaDataJSONString);

            //put wala file to url
            try {
                BaseSasPutRequest.putFileToUrl(writeUrl, BaseSasPutRequest.convertFileToBytes(outZip), false);
                //delete local wala file
                outZip.delete();

            }
            catch(TransientException e) {
                this.putCallFailedPreviously=true;
                throw e;
            }

            //this.putCallSucceeded=true;
            //this.putCallFailedPreviously=false;
        }


        if(!finalizeSucceeded) {

            ChatwalaResponse<ChatwalaMessage> completeResponse = (ChatwalaResponse<ChatwalaMessage>) new CompleteReplyMessageRequest(context, newMessageId).execute();

            if(completeResponse.getResponseCode()!=1) {
                throw new TransientException();
            }

            //no longer need the video file, delete it
            new File(videoFilePath).delete();

            this.finalizeSucceeded=true;
        }

        if(!markReadSucceeded) {
            Dao<ChatwalaMessage, String> messageDao = null;
            try {
                messageDao = DatabaseHelper.getInstance(context).getChatwalaMessageDao();
                ChatwalaMessage reply = messageDao.queryForId(replyingToMessage.getMessageId());
                reply.setMessageState(ChatwalaMessage.MessageState.REPLIED);
                messageDao.update(reply);
                BroadcastSender.makeNewMessagesBroadcast(context);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            this.markReadSucceeded=true;

        }

    }
}
