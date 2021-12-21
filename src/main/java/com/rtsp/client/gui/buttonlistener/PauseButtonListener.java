package com.rtsp.client.gui.buttonlistener;

import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PauseButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        //MediaPanel mediaPanel = GuiManager.getInstance().getMediaPanel();
        //mediaPanel.getMediaPlayer().pause();

        //테스트용
        GuiManager.getInstance().getControlPanel().applyPauseButtonStatus();

        // Send PAUSE
        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit == null) {
            return;
        }

        RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnit.getRtspUnitId());
        if (rtspNettyChannel != null) {
            rtspNettyChannel.sendPause(rtspUnit);
        }
    }
}
