package com.chatwala.android.queue.jobs;

import com.chatwala.android.db.DatabaseHelper;
import com.chatwala.android.events.ChatwalaMessageThreadEvent;
import com.chatwala.android.events.Event;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.ChatwalaMessageThreadConversation;
import com.chatwala.android.messages.ChatwalaSentMessage;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.CwJobParams;
import com.chatwala.android.queue.Priority;
import com.chatwala.android.util.CwResult;
import com.chatwala.android.util.Logger;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.path.android.jobqueue.JobManager;
import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 6/1/2014
 * Time: 2:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetConversationJob extends CwJob {
    public static final String EVENT_ID_TEMPLATE = "GetConversation%s";
    private ChatwalaMessage message;
    private ChatwalaMessageThreadConversation conversation;

    private boolean ranQuery = false;
    private boolean configuredOffsets = false;

    public static CwJob post(ChatwalaMessage message) {
        return new GetConversationJob(message).postMeToQueue();
    }

    private GetConversationJob() {}

    private GetConversationJob(ChatwalaMessage message) {
        super(String.format(EVENT_ID_TEMPLATE, message.getMessageId()),
                new CwJobParams(Priority.DOWNLOAD_IMMEDIATE_PRIORITY).requireNetwork());

        this.message = message;
        this.conversation = new ChatwalaMessageThreadConversation(message);
        conversation.getMessages().add(message);
    }

    @Override
    public String getUID() {
        return message.getMessageId();
    }

    @Override
    protected JobManager getQueueToPostTo() {
        return getDownloadQueue();
    }

    private String getInternalEventId() {
        return "_" + getEventId();
    }

    @Override
    public void onRun() throws Throwable {
        if(!ranQuery) {
            Dao<ChatwalaSentMessage, String> sentDao = DatabaseHelper.get().getChatwalaSentMessageDao();
            QueryBuilder<ChatwalaSentMessage, String> conversationQuery = sentDao.queryBuilder();
            conversationQuery.setWhere(conversationQuery.where()
                    .eq("threadId", message.getThreadId()).and()
                    .eq("threadIndex", message.getThreadIndex() - 1).or()
                    .eq("threadIndex", message.getThreadIndex() + 1));
            conversationQuery.orderBy("threadIndex", true);
            List<ChatwalaSentMessage> sentMessages = conversationQuery.query();
            if (message.getThreadIndex() > 2 && sentMessages.size() != 2) {
                Logger.w("Couldn't get two sent messages for the conversation");
                EventBus.getDefault().post(new ChatwalaMessageThreadEvent(getEventId(), Event.Extra.INVALID_CONVERSATION));
                return;
            }

            //only keep the latest sent message for a thread index
            Map<Long, ChatwalaSentMessage> dupes = new HashMap<Long, ChatwalaSentMessage>();
            for(ChatwalaSentMessage sentMessage : sentMessages) {
                ChatwalaSentMessage dupe = dupes.get(sentMessage.getThreadIndex());
                if(dupe == null) {
                    dupes.put(sentMessage.getThreadIndex(), sentMessage);
                }
                else {
                    if(sentMessage.getTimestamp() > dupe.getTimestamp()) {
                        dupes.put(sentMessage.getThreadIndex(), sentMessage);
                    }
                }
            }
            sentMessages = new ArrayList<ChatwalaSentMessage>(dupes.values());

            conversation.getSentMessages().addAll(sentMessages);
            ranQuery = true;
        }

        /**
         * this could cause multiple jobs to be posted, but the job
         * will reject any that are in flight
         * we gain the ability to recheck if the file is downloaded
         * (maybe someone else downloaded it for us while we were gone)
        */
        if(!message.isInLocalStorage()) {
            GetWalaJob.post(message, false, getInternalEventId(), Priority.DOWNLOAD_IMMEDIATE_PRIORITY);
        }
        for (ChatwalaSentMessage sentMessage : conversation.getSentMessages()) {
            if (!sentMessage.isInLocalStorage()) {
                GetSentWalaJob.post(sentMessage, getInternalEventId(), Priority.DOWNLOAD_IMMEDIATE_PRIORITY);
            }
        }

        if(!configuredOffsets) {
            conversation.getMessageOffsets().add(0);
            if(message.getThreadIndex() == 0) {
                conversation.getSentMessageOffsets().add(0);
            }
            else {
                Integer firstOffset = (int) (conversation.getSentMessages().get(0).getStartRecording() * 1000);
                //TODO this is a hotfix for the videos getting unsynced
                if(firstOffset > 1000) {
                    firstOffset -= 500;
                }
                conversation.getSentMessageOffsets().add(firstOffset);
                conversation.getSentMessageOffsets().add(0);
            }
            configuredOffsets = true;
        }

        //TODO spinning?
        while(true) {
            boolean messagesReady = true;
            if(!message.isInLocalStorage()) {
                messagesReady = false;
            }
            else {
                for(ChatwalaSentMessage sentMessage : conversation.getSentMessages()) {
                    if(!sentMessage.isInLocalStorage()) {
                        messagesReady = false;
                        break;
                    }
                }
            }

            if(messagesReady) {
                break;
            }
            else {
                Thread.sleep(3000);
            }
        }

        EventBus.getDefault().post(new ChatwalaMessageThreadEvent(getEventId(), new CwResult<ChatwalaMessageThreadConversation>(conversation)));
    }
}
