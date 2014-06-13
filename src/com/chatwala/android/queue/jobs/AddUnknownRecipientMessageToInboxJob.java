package com.chatwala.android.queue.jobs;

import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.AddUnknownRecipientMessageToInboxRequest;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.path.android.jobqueue.JobManager;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/10/2014
 * Time: 11:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddUnknownRecipientMessageToInboxJob extends CwJob {
    private ChatwalaMessage message;

    public static CwJob post(ChatwalaMessage message) {
        return new AddUnknownRecipientMessageToInboxJob(message).postMeToQueue();
    }

    private AddUnknownRecipientMessageToInboxJob() {}

    private AddUnknownRecipientMessageToInboxJob(ChatwalaMessage message) {
        super(new CwJobParams(Priority.API_LOW_PRIORITY).requireNetwork().persist());
        this.message = message;
    }

    @Override
    public String getUID() {
        return message.getMessageId();
    }

    @Override
    public void onRun() throws Throwable {
        AddUnknownRecipientMessageToInboxRequest request = new AddUnknownRecipientMessageToInboxRequest(message);
        request.log();
        CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
        NetworkLogger.log(response, response.getData().toString());
        if(response.getResponseCode() == 200) {
            //TODO do we need to do anything here - I don't think so
        }
        else {
            throw new RuntimeException("Didn't get a 200 response code...try again");
        }
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getApiQueue();
    }
}
