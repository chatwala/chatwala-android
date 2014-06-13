package com.chatwala.android.camera;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.chatwala.android.R;
import com.chatwala.android.media.CwTrack;
import com.chatwala.android.media.CwVideoTrack;
import com.chatwala.android.media.OnTrackReadyListener;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.ChatwalaMessageThreadConversation;
import com.chatwala.android.messages.ChatwalaSentMessage;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.ui.CroppingLayout;
import com.chatwala.android.util.CwAnalytics;
import com.chatwala.android.util.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 6/1/2014
 * Time: 1:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConversationViewerFragment extends ChatwalaFragment implements AudioManager.OnAudioFocusChangeListener {
    private ChatwalaPlaybackTexture topPlaybackSurface;
    private ChatwalaPlaybackTexture bottomPlaybackSurface;

    private List<ChatwalaSentMessage> topMessages;
    private List<Integer> topOffsets;
    private List<Future<VideoMetadata>> topMessageMetadataFutures;
    private ArrayList<CwTrack> topTracks;

    private List<ChatwalaMessage> bottomMessages;
    private List<Integer> bottomOffsets;
    private List<Future<VideoMetadata>> bottomMessageMetadataFutures;
    private ArrayList<CwTrack> bottomTracks;

    private AtomicInteger initialTrackReadyCount = new AtomicInteger(0);
    private AtomicInteger tracksFinishedCount = new AtomicInteger(0);
    private AtomicBoolean wasErrorShown = new AtomicBoolean(false);

    private boolean startedPlayback = false;

    private ImageView actionImage;
    private TextView bottomText;

    private ConversationViewerOnTrackReadyListener topOnTrackReadyListener = new ConversationViewerOnTrackReadyListener() {
        @Override
        public boolean onTrackReady(CwTrack track) {
            return true;
        }

        @Override
        public boolean onTracksFinished() {
            return super.onTracksFinished();
        }
    };

    private ConversationViewerOnTrackReadyListener bottomOnTrackReadyListener = new ConversationViewerOnTrackReadyListener() {
        @Override
        public boolean onTrackReady(CwTrack track) {
            return true;
        }

        @Override
        public boolean onTracksFinished() {
            topPlaybackSurface.unmute();
            bottomText = generateCwTextView("", Color.argb(175, 255, 255, 255));
            addViewToBottom(bottomText, false);
            return super.onTracksFinished();
        }
    };

    public ConversationViewerFragment() {}

    public static ConversationViewerFragment newInstance(ChatwalaMessageThreadConversation messages) {
        ConversationViewerFragment frag = new ConversationViewerFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("topMessages", messages.getSentMessages());
        args.putIntegerArrayList("topOffsets", messages.getSentMessageOffsets());
        args.putParcelableArrayList("bottomMessages", messages.getMessages());
        args.putIntegerArrayList("bottomOffsets", messages.getMessageOffsets());
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        CwAnalytics.setAnalyticsCategory(CwAnalytics.AnalyticsCategoryType.CONVERSATION_VIEWER);

        topMessages = getArguments().getParcelableArrayList("topMessages");
        topOffsets = getArguments().getIntegerArrayList("topOffsets");
        bottomMessages = getArguments().getParcelableArrayList("bottomMessages");
        bottomOffsets = getArguments().getIntegerArrayList("bottomOffsets");

        if(topMessages == null || bottomMessages == null) {
            Toast.makeText(getActivity(), "Could not load messages.", Toast.LENGTH_LONG).show();
            getCwActivity().showConversationStarter();
            return;
        }

        actionImage = new ImageView(getActivity());
        actionImage.setImageResource(R.drawable.ic_action_playback_play);
        getCwActivity().setActionView(actionImage);

        topMessageMetadataFutures = new ArrayList<Future<VideoMetadata>>(topMessages.size());
        for(ChatwalaSentMessage message : topMessages) {
            File playbackFile = message.getLocalVideoFile();
            if(playbackFile == null || !playbackFile.exists()) {
                cancelMetadataFutures(topMessageMetadataFutures);
                Toast.makeText(getActivity(), "Could not load messages.", Toast.LENGTH_LONG).show();
                getCwActivity().showConversationStarter();
                return;
            }
            else {
                topMessageMetadataFutures.add(MessageManager.getMessageVideoMetadata(playbackFile));
            }
        }

        bottomMessageMetadataFutures = new ArrayList<Future<VideoMetadata>>(bottomMessages.size());
        for(ChatwalaMessage message : bottomMessages) {
            File playbackFile = message.getLocalVideoFile();
            if(playbackFile == null || !playbackFile.exists()) {
                cancelMetadataFutures(topMessageMetadataFutures);
                cancelMetadataFutures(bottomMessageMetadataFutures);
                Toast.makeText(getActivity(), "Could not load messages.", Toast.LENGTH_LONG).show();
                getCwActivity().showConversationStarter();
                return;
            }
            else {
                bottomMessageMetadataFutures.add(MessageManager.getMessageVideoMetadata(playbackFile));
            }
        }

        post(new Runnable() {
            @Override
            public void run() {
                if(isAllMetadataReady()) {
                    onMetadataReady();
                    topMessageMetadataFutures = null;
                    bottomMessageMetadataFutures = null;
                }
                else {
                    postDelayed(this, 1000);
                }
            }
        });
    }

    private boolean isAllMetadataReady() {
        for(Future<VideoMetadata> future : topMessageMetadataFutures) {
            if(future == null || !future.isDone() || future.isCancelled()) {
                return false;
            }
        }

        for(Future<VideoMetadata> future : bottomMessageMetadataFutures) {
            if(future == null || !future.isDone() || future.isCancelled()) {
                return false;
            }
        }

        return true;
    }

    private void cancelMetadataFutures(List<Future<VideoMetadata>> futures) {
        for(Future<VideoMetadata> future : futures) {
            future.cancel(true);
        }
    }

    private void onMetadataReady() {
        try {
            topTracks = new ArrayList<CwTrack>(topMessageMetadataFutures.size());
            bottomTracks = new ArrayList<CwTrack>(bottomMessageMetadataFutures.size());

            int i = 0;
            for(Future<VideoMetadata> future : topMessageMetadataFutures) {
                topTracks.add(new CwVideoTrack(future.get(), topOffsets.get(i++)));
            }
            i = 0;
            for(Future<VideoMetadata> future : bottomMessageMetadataFutures) {
                bottomTracks.add(new CwVideoTrack(future.get(), bottomOffsets.get(i++)));
            }

            topMessageMetadataFutures = null;
            bottomMessageMetadataFutures = null;

            if (!topTracks.isEmpty() && !bottomTracks.isEmpty()) {
                final CroppingLayout topCl = new CroppingLayout(getActivity());
                topPlaybackSurface = new ChatwalaPlaybackTexture(getActivity(), topTracks, topOnTrackReadyListener);
                topPlaybackSurface.mute();
                topCl.addView(topPlaybackSurface);
                setTopView(topCl);

                final CroppingLayout bottomCl = new CroppingLayout(getActivity());
                bottomPlaybackSurface = new ChatwalaPlaybackTexture(getActivity(), bottomTracks, bottomOnTrackReadyListener);
                bottomCl.addView(bottomPlaybackSurface);
                setBottomView(bottomCl);
            }
            else {
                Toast.makeText(getActivity(), "Could not load messages.", Toast.LENGTH_LONG).show();
                getCwActivity().showConversationStarter();
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Could not load messages.", Toast.LENGTH_LONG).show();
            getCwActivity().showConversationStarter();
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

        abandonAudioFocus();

        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(topMessageMetadataFutures != null) {
            cancelMetadataFutures(topMessageMetadataFutures);
        }
        if(bottomMessageMetadataFutures != null) {
            cancelMetadataFutures(bottomMessageMetadataFutures);
        }
    }

    private void resetUi() {
        topPlaybackSurface.reset();
        bottomPlaybackSurface.reset();

        initialTrackReadyCount.set(0);
        tracksFinishedCount.set(0);

        try {
            if (!topTracks.isEmpty() && !bottomTracks.isEmpty()) {
                topPlaybackSurface.init(topTracks, topOnTrackReadyListener);
                topPlaybackSurface.mute();

                bottomPlaybackSurface.init(bottomTracks, bottomOnTrackReadyListener);
                bottomPlaybackSurface.unmute();
            }
            else {
                Toast.makeText(getActivity(), "Could not load messages.", Toast.LENGTH_LONG).show();
                getCwActivity().showConversationStarter();
                return;
            }

            if(!bottomPlaybackSurface.isPlaying()) {
                removeViewFromBottom(bottomText);
            }

            actionImage.setImageResource(R.drawable.ic_action_playback_play);

            startedPlayback = false;
        }
        catch(Exception e) {
            getCwActivity().showConversationStarter();
        }
    }

    @Override
    public void onActionButtonClicked() {
        if(topPlaybackSurface.isPlaying() || bottomPlaybackSurface.isPlaying()) {
            resetUi();
        }
        else {
            startedPlayback = true;
            getAudioFocus();
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            topPlaybackSurface.seekTo(0);
            bottomPlaybackSurface.seekTo(0);
            actionImage.setImageResource(R.drawable.record_stop);
            getCwActivity().setBurgerEnabled(false);

            topPlaybackSurface.start();
            bottomPlaybackSurface.start();
        }
    }

    @Override
    protected void onTopFragmentClicked() {

    }

    @Override
    protected void onBottomFragmentClicked() {
        if(!startedPlayback) {
            onActionButtonClicked();
        }
    }

    private void getAudioFocus() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Logger.w("Couldn't get audio focus");
        }
    }

    private void abandonAudioFocus() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if(topPlaybackSurface == null && bottomPlaybackSurface == null) {
            Logger.w("Couldn't init media player for when we got audio focus...abandoning audio focus");
            abandonAudioFocus();
            return;
        }

        switch(focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if(topPlaybackSurface != null) {
                    topPlaybackSurface.setVolume(1f, 1f);
                }
                if(bottomPlaybackSurface != null) {
                    bottomPlaybackSurface.setVolume(1f, 1f);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if(topPlaybackSurface != null) {
                    topPlaybackSurface.mute();
                }
                if(bottomPlaybackSurface != null) {
                    bottomPlaybackSurface.mute();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if(topPlaybackSurface != null) {
                    topPlaybackSurface.setVolume(.5f, .5f);
                }
                if(bottomPlaybackSurface != null) {
                    bottomPlaybackSurface.setVolume(.5f, .5f);
                }
                break;
            default:
                if(topPlaybackSurface != null) {
                    topPlaybackSurface.setVolume(1f, 1f);
                }
                if(bottomPlaybackSurface != null) {
                    bottomPlaybackSurface.setVolume(1f, 1f);
                }
        }
    }

    private abstract class ConversationViewerOnTrackReadyListener implements OnTrackReadyListener {
        @Override
        public final boolean onInitialTrackReady(CwTrack track) {
            int count = initialTrackReadyCount.incrementAndGet();
            if(count == 2) {
                topPlaybackSurface.seekTo(100);
                bottomPlaybackSurface.seekTo(100);
                if(getCwActivity().isMessageLoadingTimerShowing()) {
                    getCwActivity().fadeAndHideMessageLoadingTimer();
                }
                getCwActivity().setBurgerEnabled(getCwActivity().wasFirstButtonPressed());
            }
            return false;
        }

        @Override
        public abstract boolean onTrackReady(CwTrack track);

        @Override
        public boolean onTracksFinished() {
            int count = tracksFinishedCount.incrementAndGet();
            if(count == 2) {
                resetUi();
            }
            return false;
        }

        @Override
        public final void onTrackError() {
            if(wasErrorShown.compareAndSet(false, true)) {
                Toast.makeText(getActivity(), "There was an error playing back this message.", Toast.LENGTH_SHORT).show();
                getCwActivity().showConversationStarter();
            }
        }
    }
}
