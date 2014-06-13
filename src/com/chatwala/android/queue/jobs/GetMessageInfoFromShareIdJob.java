package com.chatwala.android.queue.jobs;

import com.chatwala.android.db.DatabaseHelper;
import com.chatwala.android.events.ChatwalaMessageEvent;
import com.chatwala.android.events.Event;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.GetMessageInfoFromShareIdRequest;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.chatwala.android.util.CwResult;
import com.path.android.jobqueue.JobManager;
import de.greenrobot.event.EventBus;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/9/2014
 * Time: 12:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetMessageInfoFromShareIdJob extends CwJob {
    private String shareId;

    public static CwJob post(String shareId) {
        return new GetMessageInfoFromShareIdJob(shareId).postMeToQueue();
    }

    private GetMessageInfoFromShareIdJob() {}

    private GetMessageInfoFromShareIdJob(String shareId) {
        super(shareId, new CwJobParams(Priority.API_IMMEDIATE_PRIORITY).requireNetwork());
        this.shareId = shareId;
    }

    @Override
    public String getUID() {
        return shareId;
    }

    @Override
    public void onRun() throws Throwable {
        GetMessageInfoFromShareIdRequest request = new GetMessageInfoFromShareIdRequest(shareId);
        request.log();
        CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
        NetworkLogger.log(response, response.getData().toString());
        if(response.getResponseCode() == 200) {
            if(response.getData().getJSONObject("response_code").optInt("code", -1) == 1) {
                processMessageInfo(response.getData().getString("message_id"), response.getData().getString("read_url"));
            }
            else {
                if(response.getResponseCode() == 200) {
                    EventBus.getDefault().post(new ChatwalaMessageEvent(getEventId(), Event.Extra.WALA_BAD_SHARE_ID));
                }
                else {
                    EventBus.getDefault().post(new ChatwalaMessageEvent(getEventId(), Event.Extra.WALA_STILL_PUTTING));
                }
            }
        }
        else if(response.getResponseCode() == 400) {
            EventBus.getDefault().post(new ChatwalaMessageEvent(getEventId(), Event.Extra.WALA_BAD_SHARE_ID));
        }
        else {
            throw new RuntimeException("Didn't get a 200 response code...try again");
        }
    }

    private void processMessageInfo(String messageId, String readUrl) throws Throwable {
        ChatwalaMessage message = DatabaseHelper.get().getChatwalaMessageDao().queryForId(messageId);
        if(message != null) {
            if(message.isInLocalStorage()) {
                EventBus.getDefault().post(new ChatwalaMessageEvent(getEventId(), new CwResult<ChatwalaMessage>(message)));
                return;
            }
        }
        else {
            message = new ChatwalaMessage();
            message.setMessageId(messageId);
            message.setReadUrl(readUrl);
        }
        AddUnknownRecipientMessageToInboxJob.post(message);
        GetWalaJob.post(message, false, getEventId(), Priority.DOWNLOAD_IMMEDIATE_PRIORITY);
    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new ChatwalaMessageEvent(getEventId(), Event.Extra.WALA_GENERIC_ERROR));
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getApiQueue();
    }
}
