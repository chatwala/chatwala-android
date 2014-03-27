package com.chatwala.android.networking;

import com.chatwala.android.CWResult;
import com.chatwala.android.util.Logger;

import java.net.HttpURLConnection;

/**
 * Created by Eliezer on 3/24/2014.
 */
public class NetworkLogger {
    public static void logHttpRequest(HttpURLConnection client, String url, String data) {
        Logger.i("==================HTTP REQUEST==================");
        Logger.network("Request to: " + url +
                "\n" + client.getRequestProperties().toString().replaceAll("],", "]\n") +
                "\n Sending " + data);
    }

    public static void logHttpResponse(HttpURLConnection client, String url, CWResult<?> response) {
        Logger.i("==================HTTP RESPONSE==================");
        StringBuilder sb = new StringBuilder()
                .append("Response from: ").append(url)
                .append("\n").append(client.getHeaderFields().toString().replaceAll("],", "]\n"));
        if(client.getContentType().contains("application/json") && response.getResult() != null) {
            sb.append("\n").append(response.getResult().toString());
        }
        Logger.i(sb.toString());
    }

}
