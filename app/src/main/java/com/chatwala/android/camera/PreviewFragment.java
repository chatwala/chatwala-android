package com.chatwala.android.camera;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import com.chatwala.android.R;
import com.chatwala.android.media.CwTrack;
import com.chatwala.android.media.CwVideoTrack;
import com.chatwala.android.media.OnTrackReadyListener;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.messages.MessageStartInfo;
import com.chatwala.android.util.Logger;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class PreviewFragment extends ChatwalaFragment {
    private File recordedFile;
    private boolean isReply;

    public PreviewFragment() {}

    public static PreviewFragment newInstance(VideoMetadata metadata, ChatwalaMessage replyingToMessage) {
        return newInstance(metadata, null, replyingToMessage, true, null);
    }

    public static PreviewFragment newInstance(VideoMetadata metadata, MessageStartInfo info) {
        return newInstance(metadata, info, null, false, null);
    }

    public static PreviewFragment newInstance(VideoMetadata metadata, MessageStartInfo info, String recipient) {
        return newInstance(metadata, info, null, false, recipient);
    }

    public static PreviewFragment newInstance(VideoMetadata metadata, MessageStartInfo info,
                                              ChatwalaMessage replyingToMessage, boolean isReply, String recipient) {
        PreviewFragment frag = new PreviewFragment();
        Bundle args = new Bundle();
        args.putParcelable("metadata", metadata);
        args.putParcelable("messageStartInfo", info);
        args.putParcelable("replyingToMessage", replyingToMessage);
        args.putBoolean("isReply", isReply);
        args.putString("recipient", recipient);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        isReply = getArguments().getBoolean("isReply");

        VideoMetadata metadata = getArguments().getParcelable("metadata");
        if(metadata != null) {
            recordedFile = metadata.getVideo();
            try {
                View v = LayoutInflater.from(getActivity()).inflate(R.layout.preview_fragment_top_layout, null);
                final ChatwalaPlaybackTexture pb = (ChatwalaPlaybackTexture) v.findViewById(R.id.preview_playback_surface);
                pb.init(new CwVideoTrack(metadata), new OnTrackReadyListener() {

                    @Override
                    public boolean onInitialTrackReady(CwTrack track) {
                        return true;
                    }

                    @Override
                    public boolean onTrackReady(CwTrack track) {
                        return true;
                    }

                    @Override
                    public boolean onTracksFinished() {
                        return true;
                    }

                    @Override
                    public void onTrackError() {
                        Logger.e("There was an error playing the message preview");
                        sendMessage();
                    }
                });
                setTopView(v);
                addViewToBottom(generateCwTextView(R.string.send_instructions), false);

                v.findViewById(R.id.preview_close).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        recordedFile.delete();
                        getCwActivity().showConversationStarter();
                    }
                });

                ImageView actionImage = new ImageView(getActivity());
                actionImage.setImageResource(R.drawable.ic_action_send);
                getCwActivity().setActionView(actionImage);
            }
            catch(Exception e) {
                Logger.e("There was an error showing the message preview", e);
                sendMessage();
            }
        }
    }

    @Override
    public void onActionButtonClicked() {
        sendMessage();
    }

    @Override
    protected void onTopFragmentClicked() {
        //do nothing
    }

    @Override
    protected void onBottomFragmentClicked() {
        sendMessage();

    }

    private void sendMessage() {
        if(recordedFile != null) {
            if(isReply) {
                ChatwalaMessage replyingToMessage = getArguments().getParcelable("replyingToMessage");
                MessageManager.startSendReplyMessage(replyingToMessage, recordedFile);
                Toast.makeText(getActivity(), "Message sent.", Toast.LENGTH_SHORT).show();
                getCwActivity().showConversationStarter();
            }
            else {
                String recipient = getArguments().getString("recipient");
                MessageStartInfo info = getArguments().getParcelable("messageStartInfo");
                if(recipient != null) {
                    MessageManager.startSendKnownRecipientMessage(recordedFile, info, recipient);
                    Toast.makeText(getActivity(), "Message sent.", Toast.LENGTH_SHORT).show();
                }
                else {
                    MessageManager.startSendUnknownRecipientMessage(recordedFile, info);
                    getCwActivity().sendShareUrl(info);
                }
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        getCwActivity().showConversationStarter();
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
