package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.http.BusHttpClient;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.database.DatabaseHelper;
import com.chatwala.android.util.CWLog;
import com.chatwala.android.util.MessageDataStore;
import com.crashlytics.android.Crashlytics;
import com.j256.ormlite.misc.TransactionManager;
import com.turbomanage.httpclient.AbstractRequestLogger;
import com.turbomanage.httpclient.ConsoleRequestLogger;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
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

    private static final String API_PATH_PROD       = "http://chatwala-prod.azurewebsites.net/";
    private static final String API_PATH_PROD_EAST  = "http://chatwala-prodeast.azurewebsites.net/";
    private static final String API_PATH_SANDBOX    = "http://chatwala-sandbox.azurewebsites.net/";
    private static final String API_PATH_DEV        = "http://chatwala-dev.azurewebsites.net/";
    private static final String API_PATH_DEV_EAST   = "http://chatwala-deveast.azurewebsites.net/";
    private static final String API_PATH_DUMMY      = "http://private-3a2b6-chatwalaapiversion11.apiary.io/";

    private static final String API_PATH_DEVEAST_13 = "http://chatwala-deveast-13.azurewebsites.net/";
    private static final String API_PATH_SANDBOX_13 = "http://chatwala-sandbox-13.azurewebsites.net/";

    protected Context context;

    private String clientId = "58041de0bc854d9eb514d2f22d50ad4c";
    private String clientSecret = "ac168ea53c514cbab949a80bebe09a8a";

    public BaseHttpRequest(Context context)
    {
        this.context = context;
    }

    public static String getApiPath()
    {
        return API_PATH_SANDBOX_13;
    }

    public static String getApiPathString()
    {
        if(getApiPath().equals(API_PATH_DEVEAST_13))
        {
            return "deveast_13";
        }
        else if(getApiPath().equals(API_PATH_SANDBOX_13))
        {
            return "sandbox_13";
        }
        else if(getApiPath().equals(API_PATH_PROD_EAST))
        {
            return "prodeast";
        }
        else if(getApiPath().equals(API_PATH_DEV_EAST))
        {
            return "deveast";
        }
        else if(getApiPath().equals(API_PATH_SANDBOX))
        {
            return "sandbox";
        }
        else if(getApiPath().equals(API_PATH_PROD))
        {
            return "prod";
        }
        else if(getApiPath().equals(API_PATH_DEV))
        {
            return "dev";
        }
        else if(getApiPath().equals(API_PATH_DUMMY))
        {
            return "test";
        }
        else
        {
            return "unknown";
        }
    }

    public T execute() throws TransientException, PermanentException
    {
        ChatwalaHttpClient client = new ChatwalaHttpClient(getApiPath());
        client.addHeader("x-chatwala", clientId + ":" + clientSecret);

        //Quiet the logs, some versions of intellij don't play nice with outputting bytes to the console.
//        AbstractRequestLogger logger = new AbstractRequestLogger()
//        {
//            @Override
//            public void log(String msg)
//            {
//
//            }
//        };
//        client.setRequestLogger(logger);

        HttpResponse httpResponse = makeRequest(client);

        if(httpResponse.getStatus() == STATUS_REDIRECT)
        {
            CWLog.i(BaseHttpRequest.class, "Call redirected");
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

        CWLog.i(BaseHttpRequest.class, "Request response code: " + httpResponse.getStatus());

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
