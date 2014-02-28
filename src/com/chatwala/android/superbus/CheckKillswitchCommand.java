package com.chatwala.android.superbus;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.EnvironmentVariables;
import com.chatwala.android.http.BaseHttpRequest;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.MessageDataStore;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by matthewdavis on 1/9/14.
 */
public class CheckKillswitchCommand extends SqliteCommand
{
    @Override
    public String logSummary()
    {
        return "CheckKillswitchCommand";
    }

    @Override
    public boolean same(Command command)
    {
        return command instanceof CheckKillswitchCommand;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        try
        {
            URL url = new URL(EnvironmentVariables.get().getPlistPath());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            InputStream is = new BufferedInputStream(urlConnection.getInputStream());
            File file = MessageDataStore.makePlistFile();
            FileOutputStream os = new FileOutputStream(file);

            final byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }

            os.close();
            is.close();

            if(ChatwalaApplication.isKillswitchActive(context))
            {
                throw new TransientException("Killswitch is active");
            }
            //todo: how to handle these errors, i.e. killswitch server having problems? Eat them? Act like the killswitch is set?
        }
        catch (MalformedURLException e)
        {
            Logger.e("Error getting killswitch command", e);
        }
        catch (IOException e)
        {
            Logger.e("Error getting killswitch command", e);
        }
    }

    @Override
    public int getPriority()
    {
        return Command.MUCH_HIGHER_PRIORITY;
    }
}
