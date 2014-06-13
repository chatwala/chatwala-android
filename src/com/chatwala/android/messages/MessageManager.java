package com.chatwala.android.messages;

import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.app.CwNotificationManager;
import com.chatwala.android.camera.VideoMetadata;
import com.chatwala.android.db.DatabaseHelper;
import com.chatwala.android.events.ChatwalaMessageEvent;
import com.chatwala.android.events.DrawerUpdateEvent;
import com.chatwala.android.events.Event;
import com.chatwala.android.http.CwHttpResponse;
import com.chatwala.android.http.HttpClient;
import com.chatwala.android.http.NetworkLogger;
import com.chatwala.android.http.requests.GetMessageStartInfoRequest;
import com.chatwala.android.queue.Priority;
import com.chatwala.android.queue.jobs.*;
import com.chatwala.android.util.CwResult;
import com.chatwala.android.util.Logger;
import com.chatwala.android.util.VideoUtils;
import de.greenrobot.event.EventBus;
import org.json.JSONObject;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class MessageManager {
    private static final int NUM_THREADS = 3;

    private ChatwalaApplication app;
    private final ExecutorService queue;

    private ExecutorService getQueue() {
        return queue;
    }

    private ChatwalaApplication getApp() {
        return app;
    }

    private MessageManager() {
        queue = Executors.newFixedThreadPool(NUM_THREADS);
    }

    private static class Singleton {
        public static final MessageManager instance = new MessageManager();
    }

    public static MessageManager attachToApp(ChatwalaApplication app) {
        Singleton.instance.app = app;
        return Singleton.instance;
    }

    public static MessageManager getInstance() {
        return Singleton.instance;
    }

    private static MessageManager me() {
        return Singleton.instance;
    }

    public static Future<MessageStartInfo> getNewMessageStartInfo() {
        return me().getQueue().submit(new Callable<MessageStartInfo>() {
            @Override
            public MessageStartInfo call() throws Exception {
                String messageId = UUID.randomUUID().toString();
                int retryCount = 0;
                while(retryCount < 3) {
                    try {
                        GetMessageStartInfoRequest request = new GetMessageStartInfoRequest(messageId);
                        request.log();
                        CwHttpResponse<JSONObject> response = HttpClient.getJSONObject(request);
                        NetworkLogger.log(response, response.getData().toString());
                        if (response.getData() != null && response.getData().has("share_url")) {
                            return new MessageStartInfo(messageId, response.getData().getString("share_url"));
                        } else {
                            retryCount++;
                            Logger.e("Couldn't get a message start info (retries=" + retryCount  + ")");
                        }
                    } catch (Throwable e) {
                        retryCount++;
                        Logger.e("Couldn't get a message start info (retries=" + retryCount  + ")", e);
                    }
                }
                return null;
            }
        });
    }

    public static Future<VideoMetadata> getMessageVideoMetadata(final File recordedFile) {
        return me().getQueue().submit(new Callable<VideoMetadata>() {
            @Override
            public VideoMetadata call() throws Exception {
                return VideoUtils.parseVideoMetadata(recordedFile);
            }
        });
    }

    public static void startSendUnknownRecipientMessage(final File recordedFile, final MessageStartInfo info) {
        SendUnknownRecipientMessageJob.post(recordedFile, info.getMessageId());
    }

    public static void startSendKnownRecipientMessage(final File recordedFile, final MessageStartInfo info, String recipient) {
        SendKnownRecipientMessageJob.post(recordedFile, info.getMessageId(), recipient);
    }

    public static void startSendReplyMessage(final ChatwalaMessage replyingToMessage, final File recordedFile) {
        SendReplyMessageJob.post(replyingToMessage, recordedFile);
    }

    public void startGetMessageForShareId(final String shareId) {
        GetMessageInfoFromShareIdJob.post(shareId);
    }

    public void startGetMessage(final String messageId) {
        getQueue().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ChatwalaMessage message = DatabaseHelper.get().getChatwalaMessageDao().queryForId(messageId);
                    if(message != null && message.isInLocalStorage()) {
                        EventBus.getDefault().post(new ChatwalaMessageEvent(messageId, new CwResult<ChatwalaMessage>(message)));
                    }
                    else {
                        if(message == null) {
                            EventBus.getDefault().post(new ChatwalaMessageEvent(messageId, Event.Extra.WALA_GENERIC_ERROR));
                        }
                        else {
                            GetWalaJob.post(message, false, Priority.DOWNLOAD_IMMEDIATE_PRIORITY);
                        }
                    }
                }
                catch(Exception e) {
                    Logger.e("Got an error trying to get a wala", e);
                    EventBus.getDefault().post(new ChatwalaMessageEvent(messageId, Event.Extra.WALA_GENERIC_ERROR));
                }

            }
        });
    }

    public static void startGetConversationMessages(ChatwalaMessage messageToLoad) {
        GetConversationJob.post(messageToLoad);
    }

    public static void delete(ChatwalaMessage message) {
        DeleteMessageJob.post(message);
    }

    public static void markMessageAsRead(final ChatwalaMessage message) {
        if(message.getMessageState() == MessageState.UNREAD) {
            CwNotificationManager.removeNewMessagesNotification();
            me().getQueue().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        message.setMessageState(MessageState.READ);
                        message.getDao().update(message);
                        EventBus.getDefault().post(new DrawerUpdateEvent(DrawerUpdateEvent.LOAD_EVENT_EXTRA));
                    }
                    catch(Exception e) {
                        Logger.e("There was an error marking a message as unread", e);
                    }
                }
            });
        }
    }

    public static void markAllAsRead() {
        me().getQueue().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DatabaseHelper.get().getChatwalaMessageDao().updateRaw("UPDATE message SET messageState = 'READ'");
                    CwNotificationManager.removeNewMessagesNotification();
                    EventBus.getDefault().post(new DrawerUpdateEvent(DrawerUpdateEvent.REFRESH_EVENT_EXTRA));
                } catch(Exception ignore){}
            }
        });
    }
}
