package com.chatwala.android.http.requests;

import com.chatwala.android.app.EnvironmentVariables;
import com.chatwala.android.http.CwHttpRequest;
import com.chatwala.android.http.HttpMethod;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.ChatwalaSentMessage;
import com.chatwala.android.users.UserManager;
import com.koushikdutta.async.http.body.JSONObjectBody;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/12/2014
 * Time: 5:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class StartReplyMessageRequest extends CwHttpRequest {
    public StartReplyMessageRequest(ChatwalaSentMessage newMessage, ChatwalaMessage replyingToMessage, double startRecording) throws Exception {
        super(EnvironmentVariables.get().getApiPath() + "messages/startReplyMessageSend", HttpMethod.POST);

        JSONObject json = new JSONObject();
        json.put("user_id", UserManager.getUserId());
        json.put("message_id", newMessage.getMessageId());
        json.put("replying_to_message_id", replyingToMessage.getMessageId());
        json.put("start_recording", startRecording);

        setBody(new JSONObjectBody(json));
    }
}
