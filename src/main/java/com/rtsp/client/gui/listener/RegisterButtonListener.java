package com.rtsp.client.gui.listener;

import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspRegisterNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.service.AppInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegisterButtonListener implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(RegisterButtonListener.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        // Send Register
        RtspRegisterNettyChannel rtspRegisterNettyChannel = NettyChannelManager.getInstance().getRegisterChannel();
        if (rtspRegisterNettyChannel == null) {
            return;
        }

        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit == null) {
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            rtspUnit = RtspManager.getInstance().openRtspUnit(
                    configManager.getTargetRtspIp(),
                    configManager.getTargetRtspPort()
            );
            if (rtspUnit == null) {
                log.warn("Fail to register. URI is null.");
                return;
            }
        }

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        rtspRegisterNettyChannel.sendRegister(
                rtspUnit.getRtspUnitId(),
                configManager.getTargetIp(),
                configManager.getTargetPort(),
                null
        );
    }
}
