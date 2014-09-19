package com.chatwala.android.queue;

import com.chatwala.android.messages.ChatwalaMessageBase;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.path.android.jobqueue.BaseJob;
import com.path.android.jobqueue.persistentQueue.sqlite.SqliteJobQueue;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/8/2014
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class GsonJobSerializer implements SqliteJobQueue.JobSerializer {
    private Gson gson;

    public GsonJobSerializer() {
        gson = new GsonBuilder().registerTypeAdapter(ChatwalaMessageBase.class,
                new ChatwalaMessageBase.GsonSerializerAdapter()).create();
    }

    @Override
    public byte[] serialize(Object o) throws IOException {
        if(o == null) {
            return null;
        }

        return (o.getClass().getName() + "|" + gson.toJson(o, o.getClass())).getBytes();
    }

    @Override
    @SuppressWarnings("deprecated, unchecked")
    public <T extends BaseJob> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        Class<?> clazz;
        String s = new String(bytes);
        try {
            StringBuilder sb = new StringBuilder();

            int i;
            for(i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if(c == '|') {
                    break;
                }
                else {
                    sb.append(c);
                }
            }
            s = s.substring(i + 1);

            clazz = Class.forName(sb.toString());
        }
        catch(Exception e) {
            return null;
        }

        return (T) gson.fromJson(s, clazz);
    }
}
