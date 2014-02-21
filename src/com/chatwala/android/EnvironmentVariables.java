package com.chatwala.android;

/**
 * Created by Eliezer on 2/19/14.
 */
public enum EnvironmentVariables {

    PROD(
            "https://chatwala-prodeast-13.azurewebsites.net/",
            "prodeast_13",
            "https://s3.amazonaws.com/chatwala.groundcontrol/defaults1_4.plist",
            "http://chatwala.com/?",
            "UA-46207837-1"
    ),

    QA(
            "https://chatwala-qa-13.azurewebsites.net/",
            "qa_13",
            "https://s3.amazonaws.com/chatwala.groundcontrol/QAdefaults1_4.plist",
            "http://chatwala.com/qa/?",
            "UA-46207837-4"
    ),

    DEV(
            "https://chatwala-deveast-13.azurewebsites.net/",
            "deveast_13",
            "https://s3.amazonaws.com/chatwala.groundcontrol/DEVdefaults1_4.plist",
            "http://chatwala.com/dev/?",
            "UA-46207837-3"
    ),

    SANDBOX(
            "https://chatwala-sandbox-13.azurewebsites.net/",
            "sandbox_13",
            "https://s3.amazonaws.com/chatwala.groundcontrol/DEVdefaults1_4.plist",
            "http://chatwala.com/?",
            "UA-46207837-3"
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

    private String apiPath, displayString, plistPath, webPath, googleAnalyticsID;

    EnvironmentVariables(String apiPath, String displayString, String plistPath, String webPath, String googleAnalyticsID)
    {
        this.apiPath = apiPath;
        this.displayString = displayString;
        this.plistPath = plistPath;
        this.webPath = webPath;
        this.googleAnalyticsID = googleAnalyticsID;
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

    public String getGoogleAnalyticsID()
    {
        return googleAnalyticsID;
    }
}
