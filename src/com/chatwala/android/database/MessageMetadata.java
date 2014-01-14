package com.chatwala.android.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/14/13
 * Time: 1:58 PM
 * To change this template use File | Settings | File Templates.
 */
@DatabaseTable(tableName = "message_metadata")
public class MessageMetadata
{
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField
    public int threadIndex;

    @DatabaseField
    public String threadId;

    @DatabaseField
    public String messageId;

    @DatabaseField
    public String senderId;

    @DatabaseField
    public String versionId = "1.0";

    @DatabaseField
    public String recipientId;

    @DatabaseField
    public String timestamp;

    @DatabaseField
    public double startRecording;

    public MessageMetadata()
    {
        id = 0;
        timestamp = Long.toString(System.currentTimeMillis());
    }

    public void init(JSONObject json) throws JSONException
    {
        threadIndex = json.getInt("thread_index");
        threadId = json.getString("thread_id");
        messageId = json.getString("message_id");
        senderId = json.getString("sender_id");
        versionId = json.getString("version_id");
        recipientId = json.getString("recipient_id");
        timestamp = json.getString("timestamp");
        startRecording = json.getDouble("start_recording");
    }

    public JSONObject toJson() throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put("thread_index", threadIndex);
        json.put("thread_id", threadId);
        json.put("message_id", messageId);
        json.put("sender_id", senderId);
        json.put("version_id", versionId);
        json.put("recipient_id", recipientId);
        json.put("timestamp", timestamp);
        json.put("start_recording", startRecording);

        return json;
    }

    public String toJsonString() throws JSONException
    {
        String jsonString = toJson().toString(4);

        StringBuilder sb = new StringBuilder();
        sb.append(jsonString.substring(0, jsonString.lastIndexOf('}')));
        addNull(jsonString, "sender_id", sb);
        addNull(jsonString, "recipient_id", sb);
        addNull(jsonString, "timestamp", sb);

        return sb.append("}").toString();
    }

    private void addNull(String jsonString, String key, StringBuilder sb)
    {
        if(!jsonString.contains(key))
            sb.append(",").append('"').append(key).append("\": null");
    }

    public void incrementForNewMessage()
    {
        if(threadId == null)
        {
            threadId = UUID.randomUUID().toString();
            threadIndex = 0;
        }
        else
        {
            threadIndex++;
        }
    }

    public MessageMetadata makeNew()
    {
        MessageMetadata dup = new MessageMetadata();
        dup.threadIndex = threadIndex;
        dup.threadId = threadId;
        return dup;
    }
}