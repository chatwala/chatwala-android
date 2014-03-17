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
import com.chatwala.android.superbus.PutUserProfilePictureCommand;
import com.chatwala.android.util.MessageDataStore;
import com.chatwala.android.util.ThumbUtils;
import com.chatwala.android.util.ZipUtil;
import com.chatwala.android.util.Logger;

import java.io.File;

import com.chatwala.android.http.server20.StartUnknownRecipientMessageRequest;


/**
 * Created by samirahman on 3/13/14.
 */
public class NewMessageFlowCommand extends SqliteCommand {

    String newMessageId;
    String messageMetaDataJSONString;
    String writeUrl;

    String videoFilePath;

    boolean startCallSucceeded=false;
    boolean thumbSucceeded=false;
    boolean putCallSucceeded=false;
    boolean finalizeSucceeded=false;
    boolean putCallFailedPreviously=false;

    public NewMessageFlowCommand() {}

    public NewMessageFlowCommand(String newMessageId, String videoFilePath){
       Logger.e("MO why?!!");
        this.videoFilePath = videoFilePath;
        this.newMessageId=newMessageId;
        Logger.e("MO why??222");
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

        Logger.e("MO, complete1");
        if(!startCallSucceeded) {
            Logger.e("MO, start call");
            ChatwalaResponse<ChatwalaMessage> startResponse = (ChatwalaResponse<ChatwalaMessage>) new StartUnknownRecipientMessageRequest(context, newMessageId).execute();
            ChatwalaMessage message = startResponse.getResponseData();
            this.writeUrl = message.getWriteUrl();
            this.startCallSucceeded=true;
            this.messageMetaDataJSONString = message.getMessageMetaDataString();
        }

        Logger.e("MO, complete2");
        if(!thumbSucceeded) {
            Logger.e("MO, thumb stuff");
            //thumbnail
            final File thumbFile = ThumbUtils.createThumbFromFirstFrame(context, videoFilePath);

            //create first profile picture if needed
            if(!MessageDataStore.findUserImageInLocalStore(AppPrefs.getInstance(context).getUserId()).exists())
            {
                final Context applicationContext = context.getApplicationContext();

                DataProcessor.runProcess(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        BusHelper.submitCommandSync(applicationContext, new PutUserProfilePictureCommand(thumbFile.getPath(), false));
                    }
                });

            }

            this.thumbSucceeded=true;
        }

        Logger.e("MO, complete3");
        if(putCallFailedPreviously) {
            Logger.e("MO, renew");
            ChatwalaResponse<String> renewResponse = (ChatwalaResponse<String>) new RenewWriteUrlForMessageRequest(context, newMessageId).execute();
            writeUrl = renewResponse.getResponseData();
        }

        Logger.e("MO, complete4");
        if(!putCallSucceeded) {

            Logger.e("MO, put");
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
        Logger.e("MO, complete5");

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
