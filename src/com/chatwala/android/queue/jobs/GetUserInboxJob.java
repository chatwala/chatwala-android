package com.chatwala.android.queue.jobs;

import com.chatwala.android.db.DatabaseHelper;
import com.chatwala.android.files.FileManager;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.GetUserInboxRequest;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.path.android.jobqueue.JobManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/14/2014
 * Time: 2:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserInboxJob extends CwJob {
    public static CwJob post() {
        return new GetUserInboxJob().postMeToQueue();
    }

    private GetUserInboxJob() {
        super(new CwJobParams(Priority.API_MID_PRIORITY).requireNetwork());
    }

    @Override
    public String getUID() {
        return "userInbox";
    }

    @Override
    public void onRun() throws Throwable {
        if(FileManager.getTotalAvailableSpaceInMb() < 100) {
            FileManager.clearMessageStorage();
            if(FileManager.getTotalAvailableSpaceInMb() < 100) {
                //TODO send event that leads to user being notified that they don't have enough storage space
                return;
            }
        }

        GetUserInboxRequest request = new GetUserInboxRequest();
        request.log();
        CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
        NetworkLogger.log(response, "<user inbox responses are too big to log>");

        Dao<ChatwalaMessage, String> dao = DatabaseHelper.get().getChatwalaMessageDao();
        JSONArray inbox = response.getData().getJSONArray("messages");
        Map<String, ChatwalaMessage> idMap = new HashMap<String, ChatwalaMessage>();
        for(int i = 0; i < inbox.length(); i++) {
            ChatwalaMessage message = new ChatwalaMessage(inbox.getJSONObject(i));
            idMap.put(message.getMessageId(), message);
        }

        QueryBuilder<ChatwalaMessage, String> query = dao.queryBuilder();
        query.setWhere(query.where().in("messageId", idMap.keySet()));
        query.orderBy("messageId", true);
        List<ChatwalaMessage> messages = query.query();

        for(String id : idMap.keySet()) {
            int index = Collections.binarySearch(messages, new ChatwalaMessage(id), new Comparator<ChatwalaMessage>() {
                @Override
                public int compare(ChatwalaMessage lhs, ChatwalaMessage rhs) {
                    return lhs.getMessageId().compareTo(rhs.getMessageId());
                }
            });

            if(index < 0) {
                ChatwalaMessage newMessage = idMap.get(id);
                GetWalaJob.post(newMessage, true);
            }
            else {
                ChatwalaMessage message = messages.get(index);
                if(!message.isWalaDownloaded()) {
                    GetWalaJob.post(message, true);
                }
                else {
                    ChatwalaMessage updatedMessage = idMap.get(id);
                    //TODO do we need to update all of this
                    message.setRecipientId(updatedMessage.getRecipientId());
                    message.setSenderId(updatedMessage.getSenderId());
                    message.setImageUrl(updatedMessage.getImageUrl());
                    message.setUserImageUrl(updatedMessage.getUserImageUrl());
                    message.setSortId(updatedMessage.getSortId());
                    message.setGroupId(updatedMessage.getGroupId());
                    message.setThreadId(updatedMessage.getThreadId());
                    message.setThreadIndex(updatedMessage.getThreadIndex());
                    message.setReadUrl(updatedMessage.getReadUrl());
                    message.setTimestamp(updatedMessage.getTimestamp());
                    message.setReplyingToMessageId(updatedMessage.getReplyingToMessageId());
                    dao.update(message);
                }
            }
        }
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getApiQueue();
    }
}
