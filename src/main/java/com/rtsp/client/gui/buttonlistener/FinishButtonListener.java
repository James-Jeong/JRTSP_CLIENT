package com.rtsp.client.gui.buttonlistener;

import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspRegisterNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.service.AppInstance;
import com.rtsp.client.service.ServiceManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FinishButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit != null) {
            RtspRegisterNettyChannel rtspRegisterNettyChannel = NettyChannelManager.getInstance().getRegisterChannel();
            if (rtspRegisterNettyChannel != null) {
                ConfigManager configManager = AppInstance.getInstance().getConfigManager();
                rtspRegisterNettyChannel.sendUnRegister(
                        rtspUnit.getRtspUnitId(),
                        configManager.getTargetIp(),
                        configManager.getTargetPort()
                );
            }

            RtspManager.getInstance().closeRtspUnit();
        }

        ServiceManager.getInstance().stop();
        System.exit(1);
    }
}
