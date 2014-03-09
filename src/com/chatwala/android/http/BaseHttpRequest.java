package com.chatwala.android.http;

import android.content.Context;
import android.content.pm.PackageManager;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.http.BusHttpClient;
import com.chatwala.android.EnvironmentVariables;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.util.Logger;
import com.crashlytics.android.Crashlytics;
import com.j256.ormlite.misc.TransactionManager;
import com.turbomanage.httpclient.AbstractRequestLogger;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 12:32 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseHttpRequest<T>
{
    private static final int    STATUS_OK                  = 200;
    private static final int    STATUS_NO_CONTENT          = 204;
    private static final int    STATUS_REDIRECT            = 302;
    private static final int    STATUS_BAD_REQUEST         = 400;
    private static final int    STATUS_UNAUTHORIZED        = 401;
    private static final int    STATUS_SERVER_ERROR        = 500;
    private static final int    STATUS_SERVICE_UNAVAILABLE = 503;

    protected Context context;

    private String clientId = "58041de0bc854d9eb514d2f22d50ad4c";
    private String clientSecret = "ac168ea53c514cbab949a80bebe09a8a";

    public BaseHttpRequest(Context context)
    {
        this.context = context;
    }

    public T execute() throws TransientException, PermanentException
    {
        ChatwalaHttpClient client = new ChatwalaHttpClient(EnvironmentVariables.get().getApiPath());
        client.addHeader("x-chatwala", clientId + ":" + clientSecret);

        //get version
        try {
            String version = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionName;
            client.addHeader("x-chatwala-appversion", version);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e("Couldn't get app version", e);
        }


        //Turn off teh fucking logs
        client.setRequestLogger(new AbstractRequestLogger() {
            @Override
            public void log(String s) {}
            @Override
            public void logRequest(HttpURLConnection uc, Object content) throws IOException {}
            @Override
            public void logResponse(HttpResponse res) {}
        });

        //do our logs the normal way
        logHttpRequest();

        HttpResponse httpResponse = makeRequest(client);

        if(httpResponse.getStatus() == STATUS_REDIRECT)
        {
            Logger.i("Call redirected");
            try
            {
                URL url = new URL(httpResponse.getUrl());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream is = new BufferedInputStream(urlConnection.getInputStream());

                ByteArrayOutputStream os = new ByteArrayOutputStream();

                final byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }

                is.close();

                httpResponse = new HttpResponse(urlConnection, os.toByteArray());
            }
            catch (MalformedURLException e)
            {
                throw new PermanentException(e);
            }
            catch (IOException e)
            {
                throw new PermanentException(e);
            }
        }

        logHttpResponse(httpResponse);

        if (httpResponse.getStatus() == STATUS_OK)
        {
            try
            {
                parseResponse(httpResponse);
                if (hasDbOperation())
                {
                    TransactionManager.callInTransaction(DatabaseHelper.getInstance(context).getConnectionSource(),
                            new Callable<Object>()
                            {
                                @Override
                                public Object call() throws Exception
                                {
                                    try
                                    {
                                        return commitResponse(DatabaseHelper.getInstance(context));
                                    } catch (SQLException sqlException)
                                    {
                                        Crashlytics.logException(sqlException);
                                        throw new PermanentException(
                                                "There was an SQL Exception when committing a response: " + sqlException
                                                        .getMessage() + " :: Caused by: " + sqlException.getCause());
                                    }
                                }
                            });
                }
                makeAssociatedRequests();
            } catch (SQLException sqlException)
            {
                Crashlytics.logException(sqlException);
                throw new PermanentException(
                        "There was an SQL Exception when starting a transaction for a response: " + sqlException.getMessage() +
                                " :: Caused by: " + sqlException.getCause());
            } catch (JSONException jsonException)
            {
                Crashlytics.logException(jsonException);
                throw new PermanentException(
                        "There was an JSON Exception when parsing a response: " + jsonException.getMessage() + " :: Caused by: " +
                                "" + jsonException.getCause());
            }
        }
        else if(httpResponse.getStatus() == STATUS_NO_CONTENT)
        {
            //do nothing, just continue on.
        }
        else if (httpResponse.getStatus() == STATUS_BAD_REQUEST)
        {
            throw new PermanentException();
        }
        else if (httpResponse.getStatus() == STATUS_UNAUTHORIZED)
        {
            throw new PermanentException();
        }
        else if (httpResponse.getStatus() == STATUS_SERVICE_UNAVAILABLE)
        {
            throw new TransientException();
        }
        else if (httpResponse.getStatus() == 0)
        {
            throw new TransientException();
        }
        else if (httpResponse.getStatus() == STATUS_SERVER_ERROR)
        {
            throw new TransientException();
        }
        else
        {
            throw new PermanentException(Integer.toString(httpResponse.getStatus()));
        }

        return getReturnValue();
    }

    private void logHttpRequest() {
        Logger.i("==================HTTP REQUEST==================");
        Logger.crashlytics("Request to: " + EnvironmentVariables.get().getApiPath() + getResourceURL());
    }

    private void logHttpResponse(HttpResponse response) {
        try {
            Logger.i("==================HTTP RESPONSE==================");
            String responseLog = "Response from: " + response.getUrl();
            responseLog += "\n" + response.getHeaders().toString().replaceAll("],", "]\n");
            if(response.getHeaders().get("Content-Type").toString().contains("application/json")) {
                responseLog += "\n" + response.getBodyAsString();
            }
            else {
                responseLog += "\n<Non-JSON response>";
            }
            Logger.i(responseLog);
        } catch(Exception e) {} //nothing to do about this now...probably a bug in their HTTP libraries
    }

    protected abstract String getResourceURL();

    protected abstract HttpResponse makeRequest(BusHttpClient client) throws PermanentException, TransientException;

    protected abstract void parseResponse(HttpResponse response) throws JSONException, SQLException, TransientException;

    protected abstract boolean hasDbOperation();

    protected T commitResponse(DatabaseHelper databaseHelper) throws SQLException
    {
        //Implement when hasDbOperation() returns true
        return null;
    }

    protected T getReturnValue()
    {
        return null;
    }

    protected void makeAssociatedRequests() throws PermanentException, TransientException
    {

    }
}
