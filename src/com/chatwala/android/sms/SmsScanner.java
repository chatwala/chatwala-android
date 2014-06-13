package com.chatwala.android.sms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.chatwala.android.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 6/2/2014
 * Time: 11:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class SmsScanner {
    private static final String DATE_COLUMN = "date";
    private static final String BODY_COLUMN = "body";

    public static List<String> getRecentChatwalaSmsLinks(Context context, long howRecent, TimeUnit unit) {
        Cursor c = null;
        try {
            Uri uri = Uri.parse("content://sms/inbox");
            long startingFromWhen = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(howRecent, unit);
            c = context.getContentResolver().query(uri,
                    new String[] {BODY_COLUMN},
                    DATE_COLUMN + " >= " + startingFromWhen +
                    " AND " + BODY_COLUMN + " LIKE '%http://chatwala.com%'",
                    null,
                    DATE_COLUMN + " DESC");
            if(c == null || !c.moveToFirst()) {
                return new ArrayList<String>(0);
            }

            ArrayList<String> links = new ArrayList<String>(c.getCount());
            do {
                try {

                    String body = c.getString(c.getColumnIndex(BODY_COLUMN));
                    Matcher match = Pattern.compile(".*?((http|https)://(www\\.|)chatwala.com/(dev/|qa/|)\\?\\S*)").matcher(body);
                    if(match.matches()) {
                        String link = match.group(1);
                        links.add(Uri.parse(link).getQuery());
                    }
                }
                catch(Exception e) {
                    Logger.e("Couldn't process scanned sms", e);
                }
            } while(c.moveToNext());

            return links;
        }
        catch(Exception e) {
            Logger.e("There was an error while scanning sms", e);
            return new ArrayList<String>(0);
        }
        finally {
            if(c != null && !c.isClosed()) {
                c.close();
            }
        }
    }
}
