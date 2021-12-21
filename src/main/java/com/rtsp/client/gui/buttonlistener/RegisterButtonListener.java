package com.rtsp.client.gui.buttonlistener;

import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.gui.component.panel.MediaPanel;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspRegisterNettyChannel;
import com.rtsp.client.protocol.register.RegisterRtspUnitReq;
import com.rtsp.client.protocol.register.base.URtspMessageType;
import com.rtsp.client.service.AppInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegisterButtonListener implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(RegisterButtonListener.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        /*RegisterRtspUnitReq registerRtspUnitReq = new RegisterRtspUnitReq(
                AppInstance.getInstance().getConfigManager().getMagicCookie(),
                URtspMessageType.REGISTER, 1, 0,
                "1234", 7200
        );

        log.debug("{}", registerRtspUnitReq);*/

        // Add register channel
        NettyChannelManager.getInstance().addRegisterChannel();

        // Send Register
        RtspRegisterNettyChannel rtspRegisterNettyChannel = NettyChannelManager.getInstance().getRegisterChannel();
        if (rtspRegisterNettyChannel == null) {
            return;
        }

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        rtspRegisterNettyChannel.sendRegister(
                configManager.getTargetIp(),
                configManager.getTargetPort(),
                null
        );
    }
}
