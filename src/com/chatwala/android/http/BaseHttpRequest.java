package com.chatwala.android.http;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.http.BusHttpClient;
import com.chatwala.android.database.DatabaseHelper;
import com.crashlytics.android.Crashlytics;
import com.j256.ormlite.misc.TransactionManager;
import com.turbomanage.httpclient.AbstractRequestLogger;
import com.turbomanage.httpclient.ConsoleRequestLogger;
import com.turbomanage.httpclient.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 12:32 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseHttpRequest
{
    private static final int    STATUS_OK                  = 200;
    private static final int    STATUS_NO_CONTENT          = 204;
    private static final int    STATUS_BAD_REQUEST         = 400;
    private static final int    STATUS_UNAUTHORIZED        = 401;
    private static final int    STATUS_SERVER_ERROR        = 500;
    private static final int    STATUS_SERVICE_UNAVAILABLE = 503;

    private static final String API_PATH_PROD     = "http://chatwala-prod.azurewebsites.net/";
    private static final String API_PATH_DEV     = "http://chatwala-dev.azurewebsites.net/";
    private static final String API_PATH_DUMMY = "http://private-3a2b6-chatwalaapiversion11.apiary.io/";

    protected Context context;

    public BaseHttpRequest(Context context)
    {
        this.context = context;
    }

    public Object execute() throws TransientException, PermanentException
    {
        BusHttpClient client = new BusHttpClient(API_PATH_DEV);
        AbstractRequestLogger logger = new AbstractRequestLogger()
        {
            @Override
            public void log(String msg)
            {

            }
        };
        client.setRequestLogger(logger);

        final HttpResponse httpResponse = makeRequest(client);

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
            throw new PermanentException();
        }

        return getReturnValue();
    }

    protected abstract String getResourceURL();

    protected abstract HttpResponse makeRequest(BusHttpClient client);

    protected abstract void parseResponse(HttpResponse response) throws JSONException, SQLException;

    protected abstract boolean hasDbOperation();

    protected Object commitResponse(DatabaseHelper databaseHelper) throws SQLException
    {
        //Implement when hasDbOperation() returns true
        return null;
    }

    protected Object getReturnValue()
    {
        return null;
    }

    protected void makeAssociatedRequests() throws PermanentException, TransientException
    {

    }
}
