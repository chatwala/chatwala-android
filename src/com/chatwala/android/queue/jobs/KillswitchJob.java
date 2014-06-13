package com.chatwala.android.queue.jobs;

import com.chatwala.android.app.AppPrefs;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.KillswitchRequest;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.chatwala.android.util.KillswitchInfo;
import com.path.android.jobqueue.JobManager;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/8/2014
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class KillswitchJob extends CwJob {
    public static CwJob post() {
        return new KillswitchJob().postMeToQueue();
    }

    private KillswitchJob() {
        super(new CwJobParams(Priority.API_LOW_PRIORITY).requireNetwork());
    }

    @Override
    public String getUID() {
        return "killswitch";
    }

    @Override
    public void onRun() throws Throwable {
        KillswitchRequest request = new KillswitchRequest();
        request.log();
        CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
        JSONObject killswitch = response.getData();
        if (response.getResponseCode() == 304) {
            //not modified
            NetworkLogger.log(response, null);
        }
        else {
            try {
                killswitch.put(KillswitchInfo.LAST_MODIFIED_KEY, response.getLastModified());
            } catch (Exception ie) {
                killswitch = new JSONObject();
            }
            AppPrefs.putKillswitch(killswitch);
            NetworkLogger.log(response, killswitch.toString());
        }
    }

    @Override
    protected int getRetryLimit() {
        return 5;
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getApiQueue();
    }
}
