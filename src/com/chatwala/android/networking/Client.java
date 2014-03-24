package com.chatwala.android.networking;

import android.content.Context;
import com.chatwala.android.CwResult;
import com.chatwala.android.EnvironmentVariables;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.NetworkUtils;

import java.util.concurrent.Callable;

/*package*/ abstract class Client<TClient, TResponse> implements Callable<CwResult<Response<TResponse>>> {
    private static final String clientId = "58041de0bc854d9eb514d2f22d50ad4c";
    private static final String clientSecret = "ac168ea53c514cbab949a80bebe09a8a";

    private final Context context;
    private boolean isCwApiRequest;
    private final String url;

    public Client(Context context, String url) {
        this.context = context;
        isCwApiRequest = !url.matches("(?i)^[a-z][\\w.+-]+://"); //if no protocol scheme, we use the base CwApi url
        if(isCwApiRequest) {
            this.url = EnvironmentVariables.get().getApiPath() + url;
        }
        else {
            this.url = url;
        }
    }

    protected final String getUrl() { return url; }

    protected final Context getContext() { return context; }

    protected abstract CwResult<TClient> initClient();

    protected abstract CwResult<Boolean> makeRequest(TClient client);

    protected abstract CwResult<Response<TResponse>> parseResponse(TClient client);

    protected String getXChatwalaKey() {
        return "x-chatwala";
    }

    protected String getXChatwala() {
        return clientId + ":" + clientSecret;
    }

    protected String getAppVersionKey() {
        return "x-chatwala-appversion";
    }

    @SuppressWarnings("ConstantConditions")
    protected String getAppVersion() {
        try {
            return getContext().getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch (Exception e) {
            Logger.e("Couldn't get app version", e);
            return "";
        }
    }

    /**
     * Set the x-chatwala and x-chatwala-appversion headers
     * for the specific client. This method will only be called
     * if
     * @param client
     */
    protected void setKeyAndVersionHeaders(TClient client) {}

    /**
     * Log the request that the client made.
     *
     * @param client the client that made the request
     */
    protected void logRequest(TClient client) {}

    /**
     * Log the response that the client receives.
     *
     * @param client the client that received the response
     */
    protected void logResponse(TClient client, CwResult<Response<TResponse>> response) {}

    @Override
    public CwResult<Response<TResponse>> call() {
        try {
            if(!NetworkUtils.isConnectedToInternet(getContext())) {
                return new CwResult<Response<TResponse>>(false, "There is no internet connection");
            }

            CwResult<TClient> initResult = initClient();
            if(initResult.isError()) {
                return new CwResult<Response<TResponse>>(initResult);
            }

            TClient client = initResult.getResult();
            if(isCwApiRequest) {
                setKeyAndVersionHeaders(client);
            }

            CwResult<Boolean> requestResult = makeRequest(client);
            if(requestResult.isError()) {
                return new CwResult<Response<TResponse>>(requestResult);
            }

            CwResult<Response<TResponse>> responseResult = parseResponse(client);
            logResponse(client, responseResult);
            return responseResult;
        }
        catch(Exception e) {
            Logger.e("Got an error", e);
            return new CwResult<Response<TResponse>>(false, "There was an error processing the request");
        }
    }
}
