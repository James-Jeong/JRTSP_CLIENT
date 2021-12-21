package com.rtsp.client.gui.buttonlistener;

import com.fsm.module.StateHandler;
import com.rtsp.client.fsm.RtspEvent;
import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.gui.component.panel.MediaPanel;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PlayButtonListener implements ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(PlayButtonListener.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        //MediaPanel mediaPanel = GuiManager.getInstance().getMediaPanel();
        //mediaPanel.getMediaPlayer().play();

        //테스트용
        GuiManager.getInstance().getControlPanel().applyPlayButtonStatus();

        // Send PLAY
        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit == null) {
            return;
        }

        RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnit.getRtspUnitId());
        if (rtspNettyChannel != null) {
            rtspNettyChannel.sendPlay(rtspUnit,
                    rtspUnit.getStartTime(),
                    rtspUnit.getEndTime()
            );
        } else {
            logger.warn("({}) Rtsp Channel is closed. Fail to process PLAY.", rtspUnit.getRtspUnitId());
            StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
            rtspStateHandler.fire(
                    RtspEvent.IDLE,
                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
            );
        }
    }
}
