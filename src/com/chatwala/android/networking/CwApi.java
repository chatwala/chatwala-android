package com.chatwala.android.networking;

import android.content.Context;
import com.chatwala.android.CwResult;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;

public class CwApi {
    /*package*/ static DefaultCwClient<JSONObject> getUserPictureUploadUrl(Context context, final String userId) {
        return new DefaultCwClient<JSONObject>(context, "user/postUserProfilePicture") {
            @Override
            protected CwResult<HttpURLConnection> initClient() {
                CwResult<HttpURLConnection> result = super.initClient();
                if(result.isError()) {
                    return result;
                }
                result.getResult().setDoInput(true);
                result.getResult().setDoOutput(true); //sets method to POST
                return result;
            }

            @Override
            protected CwResult<Boolean> makeRequest(HttpURLConnection client) {
                CwResult<Boolean> result = new CwResult<Boolean>();
                JSONObject request = new JSONObject();
                try {
                    request.put("user_id", userId);
                    client.setFixedLengthStreamingMode(request.toString().getBytes().length);
                    client.setRequestProperty("Content-Type", "application/json");
                    logRequest(client);
                    PrintWriter out = new PrintWriter(new BufferedOutputStream(client.getOutputStream()));
                    out.write(request.toString());
                    out.flush();
                    out.close();
                    return result.setSuccess(true);
                }
                catch(Exception e) {
                    result.setError("There was an error uploading the profile picture.");
                    return result;
                }
            }

            @Override
            protected CwResult<Response<JSONObject>> parseResponse(HttpURLConnection client) {
                try {
                    StringBuilder responseBuilder = new StringBuilder();
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String line;
                    while((line = in.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    in.close();

                    JSONObject responseJson = new JSONObject(responseBuilder.toString());
                    Response<JSONObject> response = new Response<JSONObject>(client.getHeaderFields(), client.getResponseCode(),
                            client.getResponseMessage(), responseJson);
                    return new CwResult<Response<JSONObject>>().setSuccess(response);
                }
                catch(Exception e) {
                    return new CwResult<Response<JSONObject>>(false, "There was an error uploading the profile picture.");
                }
            }
        };
    }

}
