package com.chatwala.android.camera;

import android.os.Bundle;
import android.widget.Toast;
import com.chatwala.android.events.ChatwalaMessageThreadEvent;
import com.chatwala.android.events.Event;
import com.chatwala.android.events.ProgressEvent;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.queue.jobs.GetConversationJob;
import de.greenrobot.event.EventBus;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 6/1/2014
 * Time: 2:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConversationLoaderFragment extends ChatwalaFragment {
    private ChatwalaMessage messageToLoad;

    public ConversationLoaderFragment() {}

    public static ConversationLoaderFragment newInstance(ChatwalaMessage messageToLoad) {
        ConversationLoaderFragment frag = new ConversationLoaderFragment();
        Bundle args = new Bundle();
        args.putParcelable("messageToLoad", messageToLoad);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        EventBus.getDefault().register(this);

        messageToLoad = getArguments().getParcelable("messageToLoad");

        MessageManager.startGetConversationMessages(messageToLoad);

        getCwActivity().showMessageLoadingTimer();
    }

    private String getEventId() {
        return String.format(GetConversationJob.EVENT_ID_TEMPLATE, messageToLoad.getMessageId());
    }

    public void onEventMainThread(ProgressEvent e) {
        if(e.getId().equals(getEventId())) {
            getCwActivity().setMessageLoadingTimerProgress(e.getProgress());
        }
    }

    public void onEventMainThread(ChatwalaMessageThreadEvent e) {
        if(e.getId().equals(getEventId())) {
            if(e.isSuccess()) {
                getCwActivity().showConversationViewer(e.getResult());
            }
            else {
                if(getCwActivity().isMessageLoadingTimerShowing()) {
                    getCwActivity().hideMessageLoadingTimer();
                }
                getCwActivity().showConversationStarter();
                if(e.getExtra() == Event.Extra.INVALID_CONVERSATION) {
                    Toast.makeText(getActivity(), "Could not load conversation. Please try again later.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        //this may crash if registration did not go through. just be safe
        try {
            EventBus.getDefault().unregister(this);
        }
        catch (Throwable ignore) {}
    }

    @Override
    public boolean onBackPressed() {
        getCwActivity().showConversationStarter();
        return true;
    }

    @Override
    public void onActionButtonClicked() {

    }

    @Override
    protected void onTopFragmentClicked() {

    }

    @Override
    protected void onBottomFragmentClicked() {

    }
}
