package com.chatwala.android.app;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public enum EnvironmentVariables {

    PROD(
            "https://chatwala-prodeast-20.azurewebsites.net/",
            "prodeast_20",
            "http://chatwalaprod.blob.core.windows.net/configs/groundcontrol.json",
            "http://chatwala.com/?",
            "http://chatwalaprod{shard}.blob.core.windows.net/messages/{message}",
            "UA-46207837-1",
            "213176338890949",
            false
    ),

    QA(
            "https://chatwala-qa-20.azurewebsites.net/",
            "qa_20",
            "http://chatwalanonprod.blob.core.windows.net/qa-configs/groundcontrol.json",
            "http://chatwala.com/qa/?",
            "http://chatwalanonprod.blob.core.windows.net/qa-messages/{message}",
            "UA-46207837-4",
            "213176338890949",
            true
    ),

    DEV(
            "https://chatwala-deveast-20.azurewebsites.net/",
            "deveast_20",
            "http://chatwalanonprod.blob.core.windows.net/dev-configs/groundcontrol.json",
            "http://chatwala.com/dev/?",
            "http://chatwalanonprod.blob.core.windows.net/dev-messages/{message}",
            "UA-46207837-3",
            "213176338890949",
            true
    ),

    SANDBOX(
            "https://chatwala-sandbox-13.azurewebsites.net/", //leave at 13 for now
            "sandbox_20",
            "http://chatwalanonprod.blob.core.windows.net/dev-configs/groundcontrol.json",
            "http://chatwala.com/?",
            "http://chatwalanonprod.blob.core.windows.net/sandbox-messages/{message}",
            "UA-46207837-3",
            "213176338890949",
            true
    );

    public static EnvironmentVariables get() {
        return EnvironmentVariables.DEV;
    }

    private String apiPath, displayString, killswitchPath, webPath, messageReadUrlTemplate, googleAnalyticsId, facebookAppId;
    private boolean isDebug;

    EnvironmentVariables(String apiPath, String displayString, String killswitchPath, String webPath, String messageReadUrlTemplate,
                         String googleAnalyticsId, String facebookAppId, boolean isDebug) {
        this.apiPath = apiPath;
        this.displayString = displayString;
        this.killswitchPath = killswitchPath;
        this.webPath = webPath;
        this.messageReadUrlTemplate = messageReadUrlTemplate;
        this.googleAnalyticsId = googleAnalyticsId;
        this.facebookAppId = facebookAppId;
        this.isDebug = isDebug;
    }

    public String getApiPath() {
        return apiPath;
    }

    public String getDisplayString() {
        return displayString;
    }

    public String getKillswitchPath() {
        return killswitchPath;
    }

    public String getWebPath() {
        return webPath;
    }

    public String getMessageReadUrlTemplate() {
        return messageReadUrlTemplate;
    }

    public String getGoogleAnalyticsId() {
        return googleAnalyticsId;
    }

    public String getFacebookAppId() {
        return facebookAppId;
    }

    public boolean isDebug() {
        return isDebug;
    }
}
