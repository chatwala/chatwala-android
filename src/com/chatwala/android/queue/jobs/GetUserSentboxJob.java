package com.chatwala.android.queue.jobs;

import com.chatwala.android.db.DatabaseHelper;
import com.chatwala.android.files.FileManager;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.GetUserSentboxRequest;
import com.chatwala.android.messages.ChatwalaSentMessage;
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
 * User: Eliezer
 * Date: 6/2/2014
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserSentboxJob extends CwJob {
    public static CwJob post() {
        return new GetUserSentboxJob().postMeToQueue();
    }

    private GetUserSentboxJob() {
        super(new CwJobParams(Priority.API_LOW_PRIORITY).requireNetwork());
    }

    @Override
    public String getUID() {
        return "userSentbox";
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

        GetUserSentboxRequest request = new GetUserSentboxRequest();
        request.log();
        CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
        NetworkLogger.log(response, "<user sentbox responses are too big to log>");

        Dao<ChatwalaSentMessage, String> dao = DatabaseHelper.get().getChatwalaSentMessageDao();
        JSONArray sentbox = response.getData().getJSONArray("messages");
        Map<String, ChatwalaSentMessage> idMap = new HashMap<String, ChatwalaSentMessage>();
        for(int i = 0; i < sentbox.length(); i++) {
            ChatwalaSentMessage sentMessage = new ChatwalaSentMessage(sentbox.getJSONObject(i));
            idMap.put(sentMessage.getMessageId(), sentMessage);
        }

        QueryBuilder<ChatwalaSentMessage, String> query = dao.queryBuilder();
        query.setWhere(query.where().in("messageId", idMap.keySet()));
        query.orderBy("messageId", true);
        List<ChatwalaSentMessage> sentMessages = query.query();

        for(String id : idMap.keySet()) {
            int index = Collections.binarySearch(sentMessages, new ChatwalaSentMessage(id), new Comparator<ChatwalaSentMessage>() {
                @Override
                public int compare(ChatwalaSentMessage lhs, ChatwalaSentMessage rhs) {
                    return lhs.getMessageId().compareTo(rhs.getMessageId());
                }
            });

            if(index < 0) {
                ChatwalaSentMessage newSentMessage = idMap.get(id);
                newSentMessage.setWalaDownloaded(true);
                dao.createOrUpdate(newSentMessage);
            }
            else {
                ChatwalaSentMessage sentMessage = sentMessages.get(index);
                ChatwalaSentMessage updatedSentMessage = idMap.get(id);
                //TODO do we need to update all of this
                sentMessage.setRecipientId(updatedSentMessage.getRecipientId());
                sentMessage.setSenderId(updatedSentMessage.getSenderId());
                sentMessage.setImageUrl(updatedSentMessage.getImageUrl());
                sentMessage.setUserImageUrl(updatedSentMessage.getUserImageUrl());
                sentMessage.setSortId(updatedSentMessage.getSortId());
                sentMessage.setGroupId(updatedSentMessage.getGroupId());
                sentMessage.setThreadId(updatedSentMessage.getThreadId());
                sentMessage.setThreadIndex(updatedSentMessage.getThreadIndex());
                sentMessage.setReadUrl(updatedSentMessage.getReadUrl());
                sentMessage.setTimestamp(updatedSentMessage.getTimestamp());
                sentMessage.setReplyingToMessageId(updatedSentMessage.getReplyingToMessageId());
                sentMessage.setWalaDownloaded(true);
                dao.update(sentMessage);
            }
        }
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getApiQueue();
    }
}
