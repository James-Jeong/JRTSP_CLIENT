package com.rtsp.client.gui.listener;

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

public class PauseButtonListener implements ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(PauseButtonListener.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        if (GuiManager.getInstance().isUploaded()) {
            GuiManager.getInstance().getVideoPanel().getMediaPlayer().pause();
            GuiManager.getInstance().getControlPanel().applyPauseButtonStatus();
            return;
        }

        // Send PAUSE
        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit == null) {
            return;
        }

        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);

        RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnit.getRtspUnitId());
        if (rtspNettyChannel != null) {
            rtspNettyChannel.sendPause(rtspUnit);
        } else {
            logger.warn("({}) Rtsp Channel is closed. Fail to process PAUSE.", rtspUnit.getRtspUnitId());
            if (rtspStateHandler != null) {
                rtspStateHandler.fire(
                        RtspEvent.IDLE,
                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                );
            }
        }
    }
}
