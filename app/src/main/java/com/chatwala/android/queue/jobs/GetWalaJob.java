package com.chatwala.android.queue.jobs;

import com.chatwala.android.app.CwNotificationManager;
import com.chatwala.android.events.BaseChatwalaMessageEvent;
import com.chatwala.android.events.ChatwalaMessageEvent;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.Priority;
import com.chatwala.android.util.CwResult;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/11/2014
 * Time: 12:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetWalaJob extends BaseGetWalaJob<ChatwalaMessage> {
    private boolean createNotificationOnDownload;

    public static CwJob post(ChatwalaMessage message, boolean createNotificationOnDownload) {
        return new GetWalaJob(message, createNotificationOnDownload).postMeToQueue();
    }

    public static CwJob post(ChatwalaMessage message, boolean createNotificationOnDownload, String eventId) {
        return new GetWalaJob(message, createNotificationOnDownload, eventId).postMeToQueue();
    }

    public static CwJob post(ChatwalaMessage message, boolean createNotificationOnDownload, Priority priority) {
        return new GetWalaJob(message, createNotificationOnDownload, priority).postMeToQueue();
    }

    public static CwJob post(ChatwalaMessage message, boolean createNotificationOnDownload, String eventId, Priority priority) {
        return new GetWalaJob(message, createNotificationOnDownload, eventId, priority).postMeToQueue();
    }

    private GetWalaJob() {}

    private GetWalaJob(ChatwalaMessage message, boolean createNotificationOnDownload) {
        this(message, createNotificationOnDownload, message.getMessageId());
    }

    private GetWalaJob(ChatwalaMessage message, boolean createNotificationOnDownload, String eventId) {
        super(eventId, message);
        this.createNotificationOnDownload = createNotificationOnDownload;
    }

    private GetWalaJob(ChatwalaMessage message, boolean createNotificationOnDownload, Priority priority) {
        this(message, createNotificationOnDownload, message.getMessageId(), priority);
    }

    private GetWalaJob(ChatwalaMessage message, boolean createNotificationOnDownload, String eventId, Priority priority) {
        super(eventId, message, priority);
        this.createNotificationOnDownload = createNotificationOnDownload;
    }

    @Override
    protected ChatwalaMessageEvent createMessageEvent(String eventId, CwResult<ChatwalaMessage> result) {
        return new ChatwalaMessageEvent(eventId, result);
    }

    @Override
    protected BaseChatwalaMessageEvent<ChatwalaMessage> createMessageEvent(String eventId, int extra) {
        return new ChatwalaMessageEvent(eventId, extra);
    }

    @Override
    protected void onWalaDownloaded(ChatwalaMessage message) {
        if(createNotificationOnDownload) {
            CwNotificationManager.makeNewMessagesNotification();
        }
    }

    @Override
    protected void deleteMessage() {
        DeleteMessageJob.post(getMessage());
    }
}