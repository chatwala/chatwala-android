package com.chatwala.android.queue.jobs;

import com.chatwala.android.events.BaseChatwalaMessageEvent;
import com.chatwala.android.events.ChatwalaSentMessageEvent;
import com.chatwala.android.messages.ChatwalaSentMessage;
import com.chatwala.android.queue.CwJob;
import com.chatwala.android.queue.NetworkConnectionChecker;
import com.chatwala.android.util.CwResult;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/12/2014
 * Time: 2:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetSentWalaJob extends BaseGetWalaJob<ChatwalaSentMessage> {
    public static CwJob post(ChatwalaSentMessage message) {
        return new GetSentWalaJob(message, message.getMessageId()).postMeToQueue();
    }

    public static CwJob post(ChatwalaSentMessage message, String eventId) {
        return new GetSentWalaJob(message, eventId).postMeToQueue();
    }

    public static CwJob post(ChatwalaSentMessage message, String eventId, int priority) {
        return new GetSentWalaJob(message, eventId, priority).postMeToQueue();
    }

    private GetSentWalaJob() {}

    private GetSentWalaJob(ChatwalaSentMessage message, String eventId) {
        super(eventId, message);
    }

    private GetSentWalaJob(ChatwalaSentMessage message, String eventId, int priority) {
        super(eventId, message, priority);
    }

    @Override
    protected ChatwalaSentMessageEvent createMessageEvent(String eventId, CwResult<ChatwalaSentMessage> result) {
        return new ChatwalaSentMessageEvent(eventId, result);
    }

    @Override
    protected BaseChatwalaMessageEvent<ChatwalaSentMessage> createMessageEvent(String eventId, int extra) {
        return new ChatwalaSentMessageEvent(eventId, extra);
    }

    @Override
    protected void onWalaDownloaded(ChatwalaSentMessage message) {

    }

    @Override
    protected void deleteMessage() {}

    @Override
    public boolean canReachRequiredNetwork() {
        return NetworkConnectionChecker.getInstance().isConnected();
    }
}
