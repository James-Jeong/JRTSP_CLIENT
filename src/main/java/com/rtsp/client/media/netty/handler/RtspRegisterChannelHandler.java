package com.rtsp.client.media.netty.handler;

import com.fsm.module.StateHandler;
import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.fsm.RtspEvent;
import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspRegisterNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.protocol.register.RegisterRtspUnitRes;
import com.rtsp.client.protocol.register.UnRegisterRtspUnitRes;
import com.rtsp.client.protocol.register.base.URtspHeader;
import com.rtsp.client.protocol.register.base.URtspMessageType;
import com.rtsp.client.service.AppInstance;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class RtspRegisterChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(RtspRegisterChannelHandler.class);

    private final String ip;
    private final int port;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspRegisterChannelHandler(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) {
        try {
            RtspRegisterNettyChannel rtspRegisterNettyChannel = NettyChannelManager.getInstance().getRegisterChannel();
            if (rtspRegisterNettyChannel == null) {
                logger.warn("RtspRegister Channel is not defined.");
                return;
            }

            ByteBuf buf = datagramPacket.content();
            if (buf == null) {
                logger.warn("DatagramPacket's content is null.");
                return;
            }

            int readBytes = buf.readableBytes();
            if (buf.readableBytes() <= 0) {
                logger.warn("Message is null.");
                return;
            }

            byte[] data = new byte[readBytes];
            buf.getBytes(0, data);

            URtspHeader uRtspHeader = new URtspHeader(data);
            if (uRtspHeader.getMessageType() == URtspMessageType.REGISTER) {
                RegisterRtspUnitRes registerRtspUnitRes = new RegisterRtspUnitRes(data);
                logger.debug("[>] {} ({})", registerRtspUnitRes, readBytes);

                RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
                if (rtspUnit == null) {
                    logger.warn("Fail to process the message. RtspUnit is null.");
                    return;
                }

                int status = registerRtspUnitRes.getStatusCode();
                if (status == RegisterRtspUnitRes.SUCCESS) { // OK
                    // RTSP Channel OPEN (New RtspUnit)
                    StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
                    rtspStateHandler.fire(
                            RtspEvent.REGISTER,
                            rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                    );

                    GuiManager.getInstance().getControlPanel().applyRegistrationButtonStatus();
                    rtspUnit.setRegistered(true);
                    rtspUnit.open();
                } else if (status == RegisterRtspUnitRes.NOT_AUTHORIZED) { // NOT AUTHORIZED
                    // KEY 를 사용하여 MD5 해싱한 값을 다시 REGISTER 에 담아서 전송
                    ConfigManager configManager = AppInstance.getInstance().getConfigManager();
                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                    messageDigest.update(registerRtspUnitRes.getRealm().getBytes(StandardCharsets.UTF_8));
                    messageDigest.update(configManager.getHashKey().getBytes(StandardCharsets.UTF_8));
                    byte[] a1 = messageDigest.digest();
                    messageDigest.reset();
                    messageDigest.update(a1);

                    String nonce = new String(messageDigest.digest());
                    rtspRegisterNettyChannel.sendRegister(
                            rtspUnit.getRtspUnitId(),
                            configManager.getTargetIp(),
                            configManager.getTargetPort(),
                            nonce
                    );
                    rtspUnit.setRegistered(false);
                }
            } else if (uRtspHeader.getMessageType() == URtspMessageType.UNREGISTER) {
                UnRegisterRtspUnitRes unRegisterRtspUnitRes = new UnRegisterRtspUnitRes(data);
                logger.debug("[>] {} ({})", unRegisterRtspUnitRes, readBytes);

                int status = unRegisterRtspUnitRes.getStatusCode();
                if (status == UnRegisterRtspUnitRes.SUCCESS) { // OK
                    GuiManager.getInstance().getControlPanel().initButtonStatus();
                    RtspManager.getInstance().closeRtspUnit();
                } else if (status == UnRegisterRtspUnitRes.NOT_ACCEPTED) { // NOT AUTHORIZED
                    RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
                    logger.warn("({}) Fail to unregister the rtspUnit.", rtspUnit.getRtspUnitId());
                }
            }
        } catch (Exception e) {
            logger.warn("RtspRegisterChannelHandler.channelRead0.Exception", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.warn("RtspRegisterChannelHandler is inactive.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("RtspRegisterChannelHandler.Exception", cause);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
