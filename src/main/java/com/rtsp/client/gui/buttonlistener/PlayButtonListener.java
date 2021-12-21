package com.rtsp.client.gui.buttonlistener;

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
            Random random = new Random();
            long sessionId = random.nextInt(RtspUnit.MAX_SESSION_ID);
            rtspUnit.setSessionId(sessionId);
            rtspNettyChannel.sendOptions(rtspUnit);
        }
    }
}
