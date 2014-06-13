package com.chatwala.android.http.requests;

import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.app.AppPrefs;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;
import com.chatwala.android.util.KillswitchInfo;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/9/2014
 * Time: 12:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class KillswitchRequest extends CwHttpRequest {
    public KillswitchRequest() throws Exception {
        super(EnvironmentVariables.get().getKillswitchPath(), HttpMethod.GET);

        KillswitchInfo killswitch = AppPrefs.getKillswitch();
        if(killswitch.getLastModified() != null) {
            setHeader("If-Modified-Since", killswitch.getLastModified());
        }
    }
}
