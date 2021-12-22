package com.rtsp.client.gui.buttonlistener;

import com.fsm.module.StateHandler;
import com.rtsp.client.fsm.RtspEvent;
import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;
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
            GuiManager.getInstance().getMediaPanel().getMediaPlayer().play();
            GuiManager.getInstance().getControlPanel().applyPlayButtonStatus();
            return;
        }

        // Send PLAY
        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit == null) {
            return;
        }

        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
        RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnit.getRtspUnitId());

        if (rtspUnit.isPaused()) {
            if (rtspNettyChannel != null) {
                rtspNettyChannel.sendPlay(rtspUnit,
                        rtspUnit.getStartTime(),
                        rtspUnit.getEndTime()
                );
            } else {
                logger.warn("({}) Rtsp Channel is closed. Fail to process PLAY. (prev: PAUSE)", rtspUnit.getRtspUnitId());
                if (rtspStateHandler != null) {
                    rtspStateHandler.fire(
                            RtspEvent.IDLE,
                            rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                    );
                }
            }
        } else {
            if (rtspNettyChannel != null) {
                Random random = new Random();
                long sessionId = random.nextInt(RtspUnit.MAX_SESSION_ID);
                rtspUnit.setSessionId(sessionId);
                rtspNettyChannel.sendOptions(rtspUnit);
            } else {
                logger.warn("({}) Rtsp Channel is closed. Fail to process OPTIONS. (prev: IDLE)", rtspUnit.getRtspUnitId());
            }
        }
    }
}
