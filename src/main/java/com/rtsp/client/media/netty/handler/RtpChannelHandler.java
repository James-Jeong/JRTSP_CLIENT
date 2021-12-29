package com.rtsp.client.media.netty.handler;

import com.rtsp.client.media.module.StreamReceiver;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.protocol.RtpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class RtpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(RtpChannelHandler.class);

    private final String rtspUnitId;
    private final String ip;
    private final int port;

    ////////////////////////////////////////////////////////////////////////////////

    public RtpChannelHandler(String rtspUnitId, String ip, int port) {
        this.rtspUnitId = rtspUnitId;
        this.ip = ip;
        this.port = port;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) {
        try {
            RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
            if (rtspUnit == null) {
                return;
            }

            ByteBuf buf = datagramPacket.content();
            if (buf == null) {
                return;
            }

            int readBytes = buf.readableBytes();
            if (buf.readableBytes() <= 0) {
                return;
            }

            byte[] data = new byte[readBytes];
            buf.getBytes(0, data);

            String dataStr = new String(data, StandardCharsets.UTF_8);
            if (dataStr.startsWith(StreamReceiver.M3U8_FILE_HEADER)) {
                rtspUnit.offerToM3U8Buffer(data);
            } else {
                RtpPacket rtpPacket = new RtpPacket(data, readBytes);
                data = rtpPacket.getPayload();
                rtspUnit.offerToTsBuffer(data);
            }
        } catch (Exception e) {
            logger.warn("RtpChannelHandler.channelRead0.Exception", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.warn("({}) RtpChannelHandler is inactive.", rtspUnitId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("({}) RtpChannelHandler.Exception (cause={})", rtspUnitId, cause.toString());
    }

    public String getRtspUnitId() {
        return rtspUnitId;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
