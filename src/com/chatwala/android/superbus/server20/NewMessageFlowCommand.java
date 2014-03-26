package com.chatwala.android.superbus.server20;

import android.content.Context;
import co.touchlab.android.superbus.BusHelper;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.database.ChatwalaMessage;
import com.chatwala.android.dataops.DataProcessor;
import com.chatwala.android.http.BaseSasPutRequest;
import com.chatwala.android.http.server20.ChatwalaResponse;
import com.chatwala.android.http.server20.CompleteUnknownRecipientMessageRequest;
import com.chatwala.android.http.server20.RenewWriteUrlForMessageRequest;
import com.chatwala.android.http.server20.StartUnknownRecipientMessageRequest;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.ThumbUtils;
import com.chatwala.android.util.ZipUtil;

import java.io.File;


/**
 * Created by samirahman on 3/13/14.
 */
public class NewMessageFlowCommand extends SqliteCommand {

    String newMessageId;
    String messageMetaDataJSONString;
    String writeUrl;
    String shardKey;
    String videoFilePath;

    boolean startCallSucceeded=false;
    boolean thumbSucceeded=false;
    boolean putCallSucceeded=false;
    boolean finalizeSucceeded=false;
    boolean putCallFailedPreviously=false;

    public NewMessageFlowCommand() {}

    public NewMessageFlowCommand(String newMessageId, String videoFilePath){
        this.videoFilePath = videoFilePath;
        this.newMessageId=newMessageId;
    }


    @Override
    public String logSummary() {
        return null;
    }

    @Override
    public boolean same(Command command) {
        if(command instanceof NewMessageFlowCommand)
        {
            return this.newMessageId.equals(((NewMessageFlowCommand) command).newMessageId);
        }

        return false;
    }


    @Override
    public void callCommand(Context context) throws TransientException, PermanentException {


        if(!new File(videoFilePath).exists()) {
            return;
        }

        if(!startCallSucceeded) {

            ChatwalaResponse<ChatwalaMessage> startResponse = (ChatwalaResponse<ChatwalaMessage>) new StartUnknownRecipientMessageRequest(context, newMessageId).execute();
            ChatwalaMessage message = startResponse.getResponseData();
            this.writeUrl = message.getWriteUrl();
            this.shardKey = message.getShardKey();
            this.startCallSucceeded=true;
            this.messageMetaDataJSONString = message.getMessageMetaDataString();
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

            this.putCallSucceeded=true;
            this.putCallFailedPreviously=false;
        }

        if(!finalizeSucceeded) {

            ChatwalaResponse<ChatwalaMessage> completeResponse = (ChatwalaResponse<ChatwalaMessage>) new CompleteUnknownRecipientMessageRequest(context, newMessageId).execute();

            if(completeResponse.getResponseCode()!=1) {
                throw new TransientException();
            }

            //no longer need the video file, delete it
            new File(videoFilePath).delete();

            this.finalizeSucceeded=true;

        }

    }
}
