package com.chatwala.android.superbus;

import android.content.Context;
import android.util.Log;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;
import com.chatwala.android.util.MessageDataStore;
import xmlwise.Plist;
import xmlwise.XmlParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

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
        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException
    {
        try
        {
            URL url = new URL("https://s3.amazonaws.com/chatwala.groundcontrol/defaults.plist");
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

            Map<String, Object> properties = Plist.load(file);
            Log.d("####KILLSWITCH####", Boolean.toString((Boolean) properties.get("APP_DISABLED")));
            Log.d("####KILLSWITCH####", (String) properties.get("APP_DISABLED_TEXT"));

            //todo: how to handle these errors, i.e. killswitch server having problems? Eat them? Act like the killswitch is set?
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (XmlParseException e)
        {
            e.printStackTrace();
        }
    }
}
