package com.chatwala.android;

/**
 * Created by Eliezer on 2/19/14.
 */
public enum EnvironmentVariables {

    PROD(
            "https://chatwala-prodeast-20.azurewebsites.net/",
            "prodeast_20",
            "https://s3.amazonaws.com/chatwala.groundcontrol/defaults1_4.plist",
            "http://chatwala.com/?",
            "http://chatwalaprod{shard}.blob.core.windows.net/messages/{message}",
            "UA-46207837-1",
            "213176338890949"
    ),

    QA(
            "https://chatwala-qa-20.azurewebsites.net/",
            "qa_20",
            "https://s3.amazonaws.com/chatwala.groundcontrol/QAdefaults1_4.plist",
            "http://chatwala.com/qa/?",
            "http://chatwalanonprod.blob.core.windows.net/qa-messages/{message}",
            "UA-46207837-4",
            "213176338890949"
    ),

    DEV(
            "https://chatwala-deveast-20.azurewebsites.net/",
            "deveast_20",
            "https://s3.amazonaws.com/chatwala.groundcontrol/DEVdefaults1_4.plist",
            "http://chatwala.com/dev/?",
            "http://chatwalanonprod.blob.core.windows.net/dev-messages/{message}",
            "UA-46207837-3",
            "213176338890949"
    ),

    SANDBOX(
            "https://chatwala-sandbox-13.azurewebsites.net/",
            "sandbox_20",
            "https://s3.amazonaws.com/chatwala.groundcontrol/DEVdefaults1_4.plist",
            "http://chatwala.com/?",
            "http://chatwalanonprod.blob.core.windows.net/sandbox-messages/{message}",
            "UA-46207837-3",
            "213176338890949"
    );

    public static EnvironmentVariables get()
    {
        return EnvironmentVariables.QA;
    }


    public static final String WEB_STRING = "http://www.chatwala.com/?";
    public static final String ALT_WEB_STRING = "http://chatwala.com/?";
    public static final String DEV_WEB_STRING = "http://chatwala.com/dev/?";
    public static final String QA_WEB_STRING = "http://chatwala.com/qa/?";
    public static final String HASH_STRING = "http://www.chatwala.com/#";
    public static final String ALT_HASH_STRING = "http://chatwala.com/#";
    public static final String REDIRECT_STRING = "http://www.chatwala.com/droidredirect.html?";

    private String apiPath, displayString, plistPath, webPath, messageReadUrlTemplate, googleAnalyticsID, facebookAppId;

    EnvironmentVariables(String apiPath, String displayString, String plistPath, String webPath, String messageReadUrlTemplate,
                         String googleAnalyticsID, String facebookAppId)
    {
        this.apiPath = apiPath;
        this.displayString = displayString;
        this.plistPath = plistPath;
        this.webPath = webPath;
        this.messageReadUrlTemplate = messageReadUrlTemplate;
        this.googleAnalyticsID = googleAnalyticsID;
        this.facebookAppId = facebookAppId;
    }

    public String getApiPath()
    {
        return apiPath;
    }

    public String getDisplayString()
    {
        return displayString;
    }

    public String getPlistPath()
    {
        return plistPath;
    }


    public String getWebPath()
    {
        return webPath;
    }

    public String getMessageReadUrlTemplate() { return messageReadUrlTemplate; }

    public String getGoogleAnalyticsID()
    {
        return googleAnalyticsID;
    }

    public String getFacebookAppId() { return facebookAppId; }
}
