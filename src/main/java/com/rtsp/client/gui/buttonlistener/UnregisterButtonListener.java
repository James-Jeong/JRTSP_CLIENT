package com.rtsp.client.gui.buttonlistener;

import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspRegisterNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.service.AppInstance;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UnregisterButtonListener implements ActionListener {

    private int number = 1;

    @Override
    public void actionPerformed(ActionEvent e) {
        // Send UnRegister
        RtspRegisterNettyChannel rtspRegisterNettyChannel = NettyChannelManager.getInstance().getRegisterChannel();
        if (rtspRegisterNettyChannel == null) {
            return;
        }

        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit == null) {
            return;
        }

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        rtspRegisterNettyChannel.sendUnRegister(
                rtspUnit.getRtspUnitId(),
                configManager.getTargetIp(),
                configManager.getTargetPort()
        );
    }
}
