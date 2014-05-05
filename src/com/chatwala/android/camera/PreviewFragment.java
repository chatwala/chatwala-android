package com.chatwala.android.camera;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import com.chatwala.android.R;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.util.Logger;

import java.io.File;

public class PreviewFragment extends ChatwalaFragment {
    private File recordedFile;
    private boolean isReply;

    public PreviewFragment() {}

    public static PreviewFragment newInstance(VideoMetadata metadata, boolean isReply) {
        PreviewFragment frag = new PreviewFragment();
        Bundle args = new Bundle();
        args.putParcelable("metadata", metadata);
        args.putBoolean("isReply", isReply);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        VideoMetadata metadata = getArguments().getParcelable("metadata");
        isReply = getArguments().getBoolean("isReply");
        if(metadata != null) {
            recordedFile = metadata.getVideo();
            try {
                View v = LayoutInflater.from(getActivity()).inflate(R.layout.preview_fragment_top_layout, null);
                final ChatwalaPlaybackTexture pb = (ChatwalaPlaybackTexture) v.findViewById(R.id.preview_playback_surface);
                pb.init(getActivity(), metadata, true);
                pb.setOnPlaybackReadyListener(new ChatwalaPlaybackTexture.OnPlaybackReadyListener() {
                    @Override
                    public void onPlaybackReady() {
                        if (pb != null) {
                            pb.start();
                        }
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
                actionImage.setImageResource(R.drawable.ic_action_send_ios);
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
                MessageManager.getInstance().sendReply(recordedFile);
            }
            else {
                MessageManager.getInstance().sendUnknownRecipientMessage(recordedFile);
            }
        }
        getCwActivity().showConversationStarter();
    }

    @Override
    public boolean onBackPressed() {
        getCwActivity().showConversationStarter();
        return true;
    }
}
