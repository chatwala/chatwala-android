package com.chatwala.android.http;

import com.chatwala.android.util.Logger;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.callback.HttpConnectCallback;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.ByteBufferListParser;
import com.koushikdutta.async.parser.JSONArrayParser;
import com.koushikdutta.async.parser.JSONObjectParser;
import com.koushikdutta.async.parser.StringParser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/8/2014
 * Time: 6:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpClient {
    public static final int DEFAULT_TIMEOUT = 15000;
    public static final int DEFAULT_FILE_TIMEOUT = 60000;
    public static final int SHORTER_FILE_TIMEOUT = 30000;

    private static HttpConnectCallback ignoreCallback = new HttpConnectCallback() {
        @Override
        public void onConnectCompleted(Exception e, AsyncHttpResponse asyncHttpResponse) {
            //do nothing because we're going to use the returned future
        }
    };

    public static AsyncHttpClient getClient() {
        return AsyncHttpClient.getDefaultInstance();
    }

    public static void request(AsyncHttpRequest request, HttpConnectCallback callback) {
        request(request, callback, DEFAULT_TIMEOUT);
    }

    public static void request(AsyncHttpRequest request, HttpConnectCallback callback, int timeout) {
        request.setTimeout(timeout);
        getClient().execute(request, callback);
    }

    public static CwHttpResponse<Void> getResponse(AsyncHttpRequest request) throws Throwable {
        return getResponse(request, DEFAULT_TIMEOUT);
    }

    public static CwHttpResponse<Void> getResponse(AsyncHttpRequest request, int timeout) throws Throwable {
        request.setTimeout(timeout);
        return new CwHttpResponse<Void>(getClient().execute(request, ignoreCallback).get(timeout, TimeUnit.MILLISECONDS));
    }

    public static void requestBlocking(AsyncHttpRequest request, HttpConnectCallback callback) throws Throwable {
        requestBlocking(request, callback, DEFAULT_TIMEOUT);
    }

    public static void requestBlocking(AsyncHttpRequest request, HttpConnectCallback callback, int timeout) throws Throwable {
        request.setTimeout(timeout);
        getClient().execute(request, callback);
    }

    public static void requestString(AsyncHttpRequest request, AsyncHttpClient.StringCallback callback) {
        requestString(request, callback, DEFAULT_TIMEOUT);
    }

    public static void requestString(AsyncHttpRequest request, AsyncHttpClient.StringCallback callback, int timeout) {
        request.setTimeout(timeout);
        getClient().executeString(request, callback);
    }

    public static CwHttpResponse<String> getString(AsyncHttpRequest request) throws Throwable {
        return getString(request, "", DEFAULT_TIMEOUT);
    }

    public static CwHttpResponse<String> getString(AsyncHttpRequest request, String defaultData) throws Throwable {
        return getString(request, defaultData, DEFAULT_TIMEOUT);
    }

    public static CwHttpResponse<String> getString(AsyncHttpRequest request, int timeout) throws Throwable {
        return getString(request, "", timeout);
    }

    public static CwHttpResponse<String> getString(AsyncHttpRequest request, String defaultData, int timeout) throws Throwable {
        request.setTimeout(timeout);
        return createResponse(getClient().execute(request, ignoreCallback), new StringParser(), defaultData);
    }

    public static void requestJSONArray(AsyncHttpRequest request, AsyncHttpClient.JSONArrayCallback callback) {
        requestJSONArray(request, callback, DEFAULT_TIMEOUT);
    }

    public static void requestJSONArray(AsyncHttpRequest request, AsyncHttpClient.JSONArrayCallback callback, int timeout) {
        request.setTimeout(timeout);
        getClient().executeJSONArray(request, callback);
    }

    public static CwHttpResponse<JSONArray> getJSONArray(AsyncHttpRequest request) throws Throwable {
        return getJSONArray(request, new JSONArray(), DEFAULT_TIMEOUT);
    }

    public static CwHttpResponse<JSONArray> getJSONArray(AsyncHttpRequest request, JSONArray defaultData) throws Throwable {
        return getJSONArray(request, defaultData, DEFAULT_TIMEOUT);
    }

    public static CwHttpResponse<JSONArray> getJSONArray(AsyncHttpRequest request, int timeout) throws Throwable {
        return getJSONArray(request, new JSONArray(), timeout);
    }

    public static CwHttpResponse<JSONArray> getJSONArray(AsyncHttpRequest request, JSONArray defaultData, int timeout) throws Throwable {
        request.setTimeout(timeout);
        return createResponse(getClient().execute(request, ignoreCallback), new JSONArrayParser(), defaultData);
    }

    public static void requestJSONObject(AsyncHttpRequest request, AsyncHttpClient.JSONObjectCallback callback) {
        requestJSONObject(request, callback, DEFAULT_TIMEOUT);
    }

    public static void requestJSONObject(AsyncHttpRequest request, AsyncHttpClient.JSONObjectCallback callback, int timeout) {
        request.setTimeout(timeout);
        getClient().executeJSONObject(request, callback);
    }

    public static CwHttpResponse<JSONObject> getJSONObject(AsyncHttpRequest request) throws Throwable {
        return getJSONObject(request, new JSONObject(), DEFAULT_TIMEOUT);
    }

    public static CwHttpResponse<JSONObject> getJSONObject(AsyncHttpRequest request, JSONObject defaultData) throws Throwable {
        return getJSONObject(request, defaultData, DEFAULT_TIMEOUT);
    }

    public static CwHttpResponse<JSONObject> getJSONObject(AsyncHttpRequest request, int timeout) throws Throwable {
        return getJSONObject(request, new JSONObject(), timeout);
    }

    public static CwHttpResponse<JSONObject> getJSONObject(AsyncHttpRequest request, JSONObject defaultData, int timeout) throws Throwable {
        request.setTimeout(timeout);
        return createResponse(getClient().execute(request, ignoreCallback), new JSONObjectParser(), defaultData);
    }

    public static void requestByteBufferList(AsyncHttpRequest request, AsyncHttpClient.DownloadCallback callback) {
        getClient().executeByteBufferList(request, callback);
    }

    public static void requestByteBufferList(AsyncHttpRequest request, AsyncHttpClient.DownloadCallback callback, int timeout) {
        request.setTimeout(timeout);
        getClient().executeByteBufferList(request, callback);
    }

    public static CwHttpResponse<ByteBufferList> getByteBufferList(AsyncHttpRequest request) throws Throwable {
        return getByteBufferList(request, new ByteBufferList(), DEFAULT_TIMEOUT);
    }

    public static CwHttpResponse<ByteBufferList> getByteBufferList(AsyncHttpRequest request, ByteBufferList defaultData) throws Throwable {
        return getByteBufferList(request, defaultData, DEFAULT_TIMEOUT);
    }

    public static CwHttpResponse<ByteBufferList> getByteBufferList(AsyncHttpRequest request, int timeout) throws Throwable {
        return getByteBufferList(request, new ByteBufferList(), timeout);
    }

    public static CwHttpResponse<ByteBufferList> getByteBufferList(AsyncHttpRequest request, ByteBufferList defaultData, int timeout) throws Throwable {
        request.setTimeout(timeout);
        return createResponse(getClient().execute(request, ignoreCallback), new ByteBufferListParser(), defaultData);
    }

    public static void requestFile(AsyncHttpRequest request, String filename, AsyncHttpClient.FileCallback callback) {
        requestFile(request, filename, callback, DEFAULT_FILE_TIMEOUT);
    }

    public static void requestFile(AsyncHttpRequest request, String filename, AsyncHttpClient.FileCallback callback, int timeout) {
        request.setTimeout(timeout);
        getClient().executeFile(request, filename, callback);
    }

    private static <T> CwHttpResponse<T> createResponse(Future<AsyncHttpResponse> rawResponseFuture,
                                                        AsyncParser<T> parser, T defaultResponse) throws Throwable {
        AsyncHttpResponse rawResponse = rawResponseFuture.get();
        CwHttpResponse<T> response = new CwHttpResponse<T>(rawResponse);
        if(response.getResponseCode() == 200) {
            T data = null;
            try {
                data = parser.parse(rawResponse).get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
            }
            catch(ExecutionException e) {
                Logger.e("There was an error parsing the response...supplying default data", e);
                data = defaultResponse;
            }
            finally {
                response.setData(data);
            }
        }
        else {
            Logger.e("Didn't get an HTTP 200 response with data " + new StringParser().parse(rawResponse).get());
        }

        return response;
    }

    //if we ever need to override android async's version of executeFile
    /*private static <T> void invoke(final RequestCallback<T> callback, SimpleFuture<T> future, final AsyncHttpResponse response, final Exception e, final T result) {
        boolean complete;
        if (e != null) {
            complete = future.setComplete(e);
        }
        else {
            complete = future.setComplete(result);
        }
        if (!complete) {
            return;
        }
        if (callback != null) {
            callback.onCompleted(e, response, result);
        }
    }

    public static void executeFile(AsyncHttpRequest request, String filename, final RequestCallback callback) {
        final File file = new File(filename);
        file.getParentFile().mkdirs();
        final OutputStream fout;
        try {
            fout = new BufferedOutputStream(new FileOutputStream(file), 8192);
        }
        catch (FileNotFoundException e) {
            return;
        }
        final FutureAsyncHttpResponse cancel = new FutureAsyncHttpResponse();
        final SimpleFuture<File> ret = new SimpleFuture<File>() {
            @Override
            public void cancelCleanup() {
                try {
                    cancel.get().setDataCallback(new NullDataCallback());
                    cancel.get().close();
                }
                catch (Exception e) {
                }
                try {
                    fout.close();
                }
                catch (Exception e) {
                }
                file.delete();
            }
        };
        ret.setParent(cancel);
        getClient().execute(request, new HttpConnectCallback() {
            long mDownloaded = 0;

            @Override
            public void onConnectCompleted(Exception ex, final AsyncHttpResponse response) {
                if (ex != null) {
                    try {
                        fout.close();
                    }
                    catch (IOException e) {
                    }
                    file.delete();
                    invoke(callback, ret, response, ex, null);
                    return;
                }
                if(response.getHeaders().getHeaders().getResponseCode() == 304) {
                    try {
                        fout.close();
                    }
                    catch (IOException e) {}
                    invoke(callback, ret, response, ex, file);
                    return;
                }
                callback.onConnect(response);

                final long contentLength = response.getHeaders().getContentLength();

                response.setDataCallback(new OutputStreamDataCallback(fout) {
                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        mDownloaded += bb.remaining();
                        super.onDataAvailable(emitter, bb);
                        callback.onProgress(response, mDownloaded, contentLength);
                    }
                });

                response.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            fout.close();
                        }
                        catch (IOException e) {
                            ex = e;
                        }
                        if (ex != null) {
                            file.delete();
                            invoke(callback, ret, response, ex, null);
                        }
                        else {
                            invoke(callback, ret, response, null, file);
                        }
                    }
                });
            }
        });
    }

    private static class FutureAsyncHttpResponse extends SimpleFuture<AsyncHttpResponse> {
        public AsyncSocket socket;
        public Object scheduled;
        public Runnable timeoutRunnable;

        @Override
        public boolean cancel() {
            if (!super.cancel())
                return false;

            if (socket != null) {
                socket.setDataCallback(new NullDataCallback());
                socket.close();
            }

            if (scheduled != null)
                getClient().getServer().removeAllCallbacks(scheduled);

            return true;
        }
    }*/
}
