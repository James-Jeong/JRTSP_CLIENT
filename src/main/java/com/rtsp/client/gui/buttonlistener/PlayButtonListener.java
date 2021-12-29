package com.rtsp.client.gui.buttonlistener;

import com.fsm.module.StateHandler;
import com.rtsp.client.fsm.RtspEvent;
import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class PlayButtonListener implements ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(PlayButtonListener.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        if (GuiManager.getInstance().isUploaded()) {
            MediaPlayer mediaPlayer = GuiManager.getInstance().getVideoPanel().getMediaPlayer();
            if (mediaPlayer == null) {
                return;
            }

            mediaPlayer.play();
            GuiManager.getInstance().getControlPanel().applyPlayButtonStatus();
            return;
        }

        // Send PLAY
        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit == null) {
            return;
        }

        if (!rtspUnit.isPaused()) {
            // GET URI FROM INPUT TEXT at the first time of playing
            String uri = GuiManager.getInstance().getSelectPlaylist();
            if (uri == null || uri.length() == 0) {
                uri = GuiManager.getInstance().getUriPanel().getUriTextField().getText();
                if (uri == null || uri.length() == 0) {
                    logger.warn("Fail to get the URI. Cannot process the play request. (uri=[{}])", uri);
                    return;
                }
            }

            uri = RtspManager.convertLocalPathToRtspPath(uri);
            if (uri.contains(" ")) {
                logger.debug("before uri: {}", uri);
                uri = uri.replaceAll("\\s", "*");
                logger.debug("after uri: {}", uri);
            }
            rtspUnit.setUri(uri);
        }

        GuiManager.getInstance().getVideoControlPanel().setVideoProgressBar(-1.0);

        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
        RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnit.getRtspUnitId());

        if (rtspUnit.isPaused()) {
            MediaPlayer mediaPlayer = GuiManager.getInstance().getVideoPanel().getMediaPlayer();
            if (mediaPlayer != null) {
                Duration duration = mediaPlayer.getCurrentTime();
                double curTime = duration.toSeconds();
                rtspUnit.setStartTime(curTime);
                if (rtspNettyChannel != null) {
                    rtspNettyChannel.sendPlay(rtspUnit,
                            rtspUnit.getStartTime(),
                            rtspUnit.getEndTime()
                    );
                } else {
                    logger.warn("({}) Fail to process PLAY. Rtsp Channel is closed. (prev: PAUSE)", rtspUnit.getRtspUnitId());
                    if (rtspStateHandler != null) {
                        rtspStateHandler.fire(
                                RtspEvent.PLAY_FAIL,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );
                    }
                    GuiManager.getInstance().getVideoControlPanel().setVideoProgressBar(0.0);
                }
            } else {
                logger.warn("({}) Fail to process PLAY. Media player is null. (prev: PAUSE)", rtspUnit.getRtspUnitId());
                if (rtspStateHandler != null) {
                    rtspStateHandler.fire(
                            RtspEvent.PLAY_FAIL,
                            rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                    );
                }
                GuiManager.getInstance().getVideoControlPanel().setVideoProgressBar(0.0);
            }
        } else {
            if (rtspNettyChannel != null) {
                Random random = new Random();
                long sessionId = random.nextInt(RtspUnit.MAX_SESSION_ID);
                rtspUnit.setSessionId(sessionId);
                rtspNettyChannel.sendOptions(rtspUnit);
            } else {
                logger.warn("({}) Rtsp Channel is closed. Fail to process OPTIONS. (prev: IDLE)", rtspUnit.getRtspUnitId());
                GuiManager.getInstance().getVideoControlPanel().setVideoProgressBar(0.0);
            }
        }
    }
}
