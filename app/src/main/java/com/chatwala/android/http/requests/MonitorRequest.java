package com.chatwala.android.http.requests;

import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 4:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class MonitorRequest extends CwHttpRequest {

    public MonitorRequest() throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "monitor", HttpMethod.GET);
    }
}
