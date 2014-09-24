package com.chatwala.android.queue.jobs;

import android.content.Context;
import com.chatwala.android.app.AppPrefs;
import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.RegisterGcmTokenRequest;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.NetworkConnectionChecker;
import com.chatwala.android.queue.Priority;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.staticbloc.jobs.JobInitializer;
import com.staticbloc.jobs.JobQueue;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/19/2014
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class RegisterGcmTokenJob extends CwJob {
    private Context context;
    private String gcmToken = null;

    public static CwJob post(Context context) {
        return new RegisterGcmTokenJob(context).postMeToQueue();
    }

    private RegisterGcmTokenJob() {}

    private RegisterGcmTokenJob(Context context) {
        super(new JobInitializer()
                .requiresNetwork(true)
                .priority(Priority.API_LOW_PRIORITY));
        this.context = context;
    }

    @Override
    public String getUID() {
        return "gcm";
    }

    @Override
    public void performJob() throws Throwable {
        if(gcmToken == null) {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
            gcmToken = gcm.register("419895337876");
        }

        RegisterGcmTokenRequest request = new RegisterGcmTokenRequest(gcmToken);
        request.log();
        CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
        NetworkLogger.log(response, response.getData().toString());

        AppPrefs.setGcmToken(gcmToken);
        AppPrefs.setGcmTokenVersion(ChatwalaApplication.getVersionCode());
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
