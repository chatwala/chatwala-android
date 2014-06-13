package com.chatwala.android.http;

import android.text.TextUtils;
import com.chatwala.android.util.Logger;
import com.koushikdutta.async.http.AsyncHttpRequest;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/8/2014
 * Time: 6:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class NetworkLogger {
    private static final int FUCKING_BIG_LOG = 4096;

    /*package*/ static void log(AsyncHttpRequest request) {
        Logger.i("==================HTTP REQUEST==================");
        String body = null;
        if(request.getBody() != null && request.getBody().get() != null) {
            body = request.getBody().get().toString();
        }
        if(TextUtils.isEmpty(body)) {
            body = "<no_request_body>";
        }
        else if(body.length() > FUCKING_BIG_LOG) {
            body = "<large_request_body>";
        }
        String requestString = request.getHeaders().getHeaders().toHeaderString();
        if(requestString.length() > 3) {
            requestString = requestString.substring(0, requestString.length() - 3);
        }
        StringBuilder sb = new StringBuilder()
                .append(requestString).append("\n")
                .append(body);
        Logger.network(sb.toString());
    }

    public static void log(CwHttpResponse<?> response, String result) {
        String responseHeaders = response.getResponseHeadersString();
        if(responseHeaders.length() > 3) {
            responseHeaders = responseHeaders.substring(0, responseHeaders.length() - 3);
        }

        Logger.i("==================HTTP RESPONSE==================");
        StringBuilder sb = new StringBuilder()
                .append(response.getUrl())
                .append("\n")
                .append(responseHeaders);
        if(response.getContentType() != null && response.getContentType().contains("application/json")) {
            if(TextUtils.isEmpty(result)) {
                result = "<no_response_body>";
            }
            else if(result.length() > FUCKING_BIG_LOG) {
                result = "<large_response_body>";
            }
            sb.append("\n").append(result);
        }
        else {
            if(response.getContentLength() == 0) {
                sb.append("\n").append("<no_response_body>");
            }
            else {
                sb.append("\n").append("<not_printing_response_body>");
            }
        }
        Logger.network(sb.toString());
    }
}
