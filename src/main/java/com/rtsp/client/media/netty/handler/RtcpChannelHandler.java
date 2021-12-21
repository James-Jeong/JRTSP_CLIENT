package com.rtsp.client.media.netty.handler;

import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.protocol.RtcpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @class public class RtcpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket>
 */
public class RtcpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(RtcpChannelHandler.class);

    private final String name;
    private final String rtspUnitId;
    private final String listenIp;
    private final int listenPort;

    ////////////////////////////////////////////////////////////////////////////////

    public RtcpChannelHandler(String rtspUnitId, String listenIp, int listenPort) {
        this.name = "RTCP_" + rtspUnitId + "_" + listenIp + ":" + listenPort;

        this.rtspUnitId = rtspUnitId;
        this.listenIp = listenIp;
        this.listenPort = listenPort;

        logger.debug("RtcpChannelHandler is created. (listenIp={}, listenPort={})", listenIp, listenPort);
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void channelRead0 (ChannelHandlerContext ctx, DatagramPacket msg) {
        try {
            RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
            if (rtspUnit == null) {
                return;
            }

            ByteBuf buf = msg.content();
            if (buf == null) {
                return;
            }

            int readBytes = buf.readableBytes();
            if (readBytes <= 0) {
                return;
            }

            byte[] data = new byte[readBytes];
            buf.getBytes(0, data);

            RtcpPacket rtcpPacket = new RtcpPacket(data);
            logger.debug("({}) {}", name, rtcpPacket);

            float fractionLost = rtcpPacket.fractionLost;
            if (fractionLost >= 0 && fractionLost <= 0.01) {
                rtspUnit.setCongestionLevel(0);
            }
            else if (fractionLost > 0.01 && fractionLost <= 0.25) {
                rtspUnit.setCongestionLevel(1);
            }
            else if (fractionLost > 0.25 && fractionLost <= 0.5) {
                rtspUnit.setCongestionLevel(2);
            }
            else if (fractionLost > 0.5 && fractionLost <= 0.75) {
                rtspUnit.setCongestionLevel(3);
            }
            else {
                rtspUnit.setCongestionLevel(4);
            }
        } catch (Exception e) {
            logger.warn("| ({}) Fail to handle the rtcp Packet.", name, e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getRtspUnitId() {
        return rtspUnitId;
    }

    public String getListenIp() {
        return listenIp;
    }

    public int getListenPort() {
        return listenPort;
    }
}
