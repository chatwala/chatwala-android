package com.chatwala.android.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import org.json.JSONObject;

/**
 * Created by Eliezer on 4/9/2014.
 */
public class KillswitchInfo {
    public static final String MINIMUM_LIVE_VERSION_KEY = "minimumLiveVersion";
    public static final String LAST_MODIFIED_KEY = "lastModified";
    public static final String COPY_OVERRIDE_KEY = "copyOverride";
    private static final String DEFAULT_COPY = "Thanks for using Chatwala, please update to the latest version in the store.";

    private Context context;
    private JSONObject killswitch;
    private int versionCode;

    public KillswitchInfo(Context context, JSONObject killswitch) {
        this.context = context;
        this.killswitch = killswitch;

        try {
            PackageInfo p = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            this.versionCode = p.versionCode;
        }
        catch(Exception e) {
            Logger.e("There was an error getting the version code", e);
            this.versionCode = Integer.MAX_VALUE;
        }
    }

    public boolean isActive() {
        return killswitch.optInt(MINIMUM_LIVE_VERSION_KEY, 0) > versionCode;
    }

    public String getLastModified() {
        return killswitch.optString(LAST_MODIFIED_KEY, null);
    }

    public String getCopy() {
        JSONObject copyOverrides = killswitch.optJSONObject(COPY_OVERRIDE_KEY);
        if(copyOverrides != null) {
            return copyOverrides.optString(Integer.toString(versionCode), DEFAULT_COPY);
        }
        return DEFAULT_COPY;
    }

    public JSONObject toJson() {
        return killswitch;
    }
}
