package com.chatwala.android.camera;

import android.os.Bundle;
import android.widget.Toast;
import com.chatwala.android.R;
import com.chatwala.android.events.ChatwalaMessageEvent;
import com.chatwala.android.events.Extras;
import com.chatwala.android.events.ProgressEvent;
import com.chatwala.android.messages.MessageManager;
import com.staticbloc.events.Events;
import com.staticbloc.events.MethodRegistration;
import com.staticbloc.events.RunType;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class MessageLoaderFragment extends ChatwalaFragment {
    private String messageVal;

    public MessageLoaderFragment() {}

    public static MessageLoaderFragment newInstance(String messageVal, boolean isShareId) {
        MessageLoaderFragment frag = new MessageLoaderFragment();
        Bundle args = new Bundle();
        args.putString("messageVal", messageVal);
        args.putBoolean("isShareId", isShareId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Events.getDefault().subscribe(this,
                new MethodRegistration<ProgressEvent>(ProgressEvent.class, "onMessageLoadProgressEvent", RunType.MAIN),
                new MethodRegistration<ChatwalaMessageEvent>(ChatwalaMessageEvent.class, "onMessageReadyEvent", RunType.MAIN));

        messageVal = getArguments().getString("messageVal");

        if(getArguments().getBoolean("isShareId")) {
            MessageManager.getInstance().startGetMessageForShareId(messageVal);
        }
        else {
            MessageManager.getInstance().startGetMessage(messageVal);
        }

        getCwActivity().showMessageLoadingTimer();
    }

    public void onMessageLoadProgressEvent(ProgressEvent e) {
        if(e.getId().equals(messageVal)) {
            getCwActivity().setMessageLoadingTimerProgress(e.getProgress());
        }
    }

    public void onMessageReadyEvent(ChatwalaMessageEvent e) {
        if(e.getId().equals(messageVal)) {
            if(e.isSuccess()) {
                getCwActivity().showConversationReplier(e.getResult());
            }
            else {
                if(getCwActivity().isMessageLoadingTimerShowing()) {
                    getCwActivity().hideMessageLoadingTimer();
                }
                getCwActivity().showConversationStarter();
                if(e.getExtra() == Extras.WALA_BAD_SHARE_ID) {
                    Toast.makeText(getActivity(), "This is not a valid message", Toast.LENGTH_LONG).show();
                }
                else if(e.getExtra() == Extras.WALA_STILL_PUTTING) {
                    Toast.makeText(getActivity(), "Your message is still downloading", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getActivity(), "There was an error downloading your message", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Events.getDefault().unsubscribe(this);
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
