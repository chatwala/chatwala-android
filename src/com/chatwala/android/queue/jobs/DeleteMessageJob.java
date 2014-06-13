package com.chatwala.android.queue.jobs;

import com.chatwala.android.events.DrawerUpdateEvent;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.DeleteMessageRequest;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.path.android.jobqueue.JobManager;
import de.greenrobot.event.EventBus;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/14/2014
 * Time: 1:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class DeleteMessageJob extends CwJob {
    private ChatwalaMessage message;

    private boolean markedAsDeleted = false;
    private boolean deleteRequestSucceeded = false;
    private boolean filesActuallyDeleted = false;
    private boolean messageDeletedFromDatabase = false;

    public static CwJob post(ChatwalaMessage message) {
        return new DeleteMessageJob(message).postMeToQueue();
    }

    private DeleteMessageJob() {}

    private DeleteMessageJob(ChatwalaMessage message) {
        super(new CwJobParams(Priority.API_MID_PRIORITY).requireNetwork().persist());
        this.message = message;
    }

    @Override
    public String getUID() {
        return message.getMessageId();
    }

    @Override
    public void onRun() throws Throwable {
        if(!markedAsDeleted) {
            message.setDeleted(true);
            message.getDao().update(message);
            markedAsDeleted = true;
        }

        if(!deleteRequestSucceeded) {
            CwHttpRequest request = new DeleteMessageRequest(message);
            request.log();
            CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
            NetworkLogger.log(response, response.getData().toString());
            if(response.getResponseCode() != 200) {
                throw new RuntimeException("The delete call did not return a 200");
            }
            deleteRequestSucceeded = true;
        }

        if(!filesActuallyDeleted) {
            message.deleteAllLocalFiles();
            filesActuallyDeleted = true;
        }

        if(!messageDeletedFromDatabase) {
            message.getDao().delete(message);
            messageDeletedFromDatabase = true;
        }

        EventBus.getDefault().post(new DrawerUpdateEvent(DrawerUpdateEvent.LOAD_EVENT_EXTRA));
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getApiQueue();
    }
}
