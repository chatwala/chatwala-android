package com.chatwala.android.networking;

import com.chatwala.android.EnvironmentVariables;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by samirahman on 3/20/14.
 */
public class ConnectionCreator {

    private static String clientId = "58041de0bc854d9eb514d2f22d50ad4c";
    private static String clientSecret = "ac168ea53c514cbab949a80bebe09a8a";

    public static HttpURLConnection createChatwalaConnection(URL url, String requestMethod) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod(requestMethod);
        urlConnection.setRequestProperty("x-chatwala", clientId + ":" + clientSecret);
        return urlConnection;
    }

    public static URL createChatwalaUrl(String path) throws MalformedURLException {
        return new URL(EnvironmentVariables.get().getApiPath() + path);
    }

}
