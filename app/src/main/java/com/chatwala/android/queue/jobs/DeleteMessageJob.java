package com.chatwala.android.queue.jobs;

import com.chatwala.android.events.DrawerUpdateEvent;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.DeleteMessageRequest;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.NetworkConnectionChecker;
import com.chatwala.android.queue.Priority;
import com.staticbloc.events.Events;
import com.staticbloc.jobs.JobInitializer;
import com.staticbloc.jobs.JobQueue;
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

    public static CwJob post(ChatwalaMessage message) {
        return new DeleteMessageJob(message).postMeToQueue();
    }

    private DeleteMessageJob() {}

    private DeleteMessageJob(ChatwalaMessage message) {
        super(new JobInitializer()
                .requiresNetwork(true)
                .isPersistent(true)
                .priority(Priority.API_MID_PRIORITY));
        this.message = message;
    }

    @Override
    public String getUID() {
        return message.getMessageId();
    }

    @Override
    public void performJob() throws Throwable {
        if(!isSubsectionComplete("markedAsDeleted")) {
            message.setDeleted(true);
            message.getDao().update(message);
            setSubsectionComplete("markedAsDeleted");
        }

        if(!isSubsectionComplete("deleteRequestSucceeded")) {
            CwHttpRequest request = new DeleteMessageRequest(message);
            request.log();
            CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
            NetworkLogger.log(response, response.getData().toString());
            if(response.getResponseCode() != 200) {
                throw new RuntimeException("The delete call did not return a 200");
            }
            setSubsectionComplete("deleteRequestSucceeded");
        }

        if(!isSubsectionComplete("filesActuallyDeleted")) {
            message.deleteAllLocalFiles();
            setSubsectionComplete("filesActuallyDeleted");
        }

        if(!isSubsectionComplete("messageDeletedFromDatabase")) {
            message.getDao().delete(message);
            setSubsectionComplete("messageDeletedFromDatabase");
        }

        Events.getDefault().post(new DrawerUpdateEvent(DrawerUpdateEvent.LOAD_EVENT_EXTRA));
    }

    @Override
    protected JobQueue getQueueToPostTo() {
        return getApiQueue();
    }

    @Override
    public boolean canReachRequiredNetwork() {
        return NetworkConnectionChecker.getInstance().isConnected();
    }
}
