package com.chatwala.android.camera;

import android.os.Bundle;
import android.widget.ImageView;
import com.chatwala.android.R;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.ui.CroppingLayout;
import com.chatwala.android.util.Logger;

import java.io.File;

public class PreviewFragment extends ChatwalaFragment {
    private File recordedFile;

    public PreviewFragment() {}

    public static PreviewFragment newInstance(VideoMetadata metadata) {
        PreviewFragment frag = new PreviewFragment();
        Bundle args = new Bundle();
        args.putParcelable("metadata", metadata);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        VideoMetadata metadata = getArguments().getParcelable("metadata");
        if(metadata != null) {
            recordedFile = metadata.getVideo();
            try {
                final CroppingLayout cl = new CroppingLayout(getActivity());
                final ChatwalaPlaybackTexture pb;
                pb = new ChatwalaPlaybackTexture(getActivity(), metadata, true);
                pb.setOnPlaybackReadyListener(new ChatwalaPlaybackTexture.OnPlaybackReadyListener() {
                    @Override
                    public void onPlaybackReady() {
                        if (pb != null) {
                            pb.start();
                        }
                    }
                });
                cl.addView(pb);
                pb.forceOnMeasure();
                setTopView(cl);
                addViewToBottom(generateCwTextView(R.string.send_instructions), false);

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
            MessageManager.getInstance().sendUnknownRecipientMessage(recordedFile);
        }
        getCwActivity().showConversationStarter();
    }

    @Override
    public boolean onBackPressed() {
        getCwActivity().showConversationStarter();
        return true;
    }
}
