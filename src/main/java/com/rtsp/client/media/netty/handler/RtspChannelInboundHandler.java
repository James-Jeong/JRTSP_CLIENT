package com.rtsp.client.media.netty.handler;

import com.fsm.module.StateHandler;
import com.rtsp.client.fsm.RtspEvent;
import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.media.sdp.base.Sdp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * @class public class RtspChannelHandler extends ChannelInboundHandlerAdapter
 * @brief RtspChannelHandler class
 * HTTP 는 TCP 연결이므로 매번 연결 상태가 변경된다. (연결 생성 > 비즈니스 로직 처리 > 연결 해제)
 */
public class RtspChannelInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RtspChannelInboundHandler.class);

    private final String name;

    private final String rtspUnitId; // rtspUnitId
    private final String listenIp; // local ip
    private final int listenRtspPort; // local(listen) rtsp port

    ////////////////////////////////////////////////////////////////////////////////

    public RtspChannelInboundHandler(String rtspUnitId, String listenIp, int listenRtspPort) {
        this.name = "RTSP_" + rtspUnitId + "_" + listenIp + ":" + listenRtspPort;
        this.rtspUnitId = rtspUnitId;
        this.listenIp = listenIp;
        this.listenRtspPort = listenRtspPort;

        logger.debug("({}) RtspChannelHandler is created. (listenIp={}, listenRtspPort={})", name, listenIp, listenRtspPort);
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) {
        try {
            RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
            if (rtspUnit == null) {
                logger.warn("({}) Fail to get the rtsp unit. RtspUnit is null.", name);
                return;
            }
            StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
            String curState = rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId()).getCurState();

            if (msg instanceof DefaultLastHttpContent) { // SDP Parsing
                if (curState.equals(RtspState.SDP_READY)) {
                    DefaultLastHttpContent defaultLastHttpContent = (DefaultLastHttpContent) msg;
                    ByteBuf buf = defaultLastHttpContent.content();
                    if (buf == null) {
                        logger.warn("({}) ({}) () Fail to process DESCRIBE. Fail to parse the sdp. Content is null.", name, rtspUnit.getRtspUnitId());
                        NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                        rtspStateHandler.fire(
                                RtspEvent.IDLE,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );
                        return;
                    }

                    int readBytes = buf.readableBytes();
                    if (buf.readableBytes() <= 0) {
                        return;
                    }
                    byte[] data = new byte[readBytes];
                    buf.getBytes(0, data);

                    String sdpStr = new String(data, StandardCharsets.UTF_8);
                    if (sdpStr.length() > 0) {
                        if (rtspUnit.parseSdp(sdpStr)) {
                            Sdp sdp = rtspUnit.getSdp();
                            logger.debug("({}) ({}) RECV SDP: {}", name, rtspUnitId, sdp.getData(true));

                            int rtpPort = sdp.getMediaPort(Sdp.VIDEO);
                            rtspUnit.setListenRtpPort(rtpPort);
                            rtspUnit.setListenRtcpPort(rtpPort + 1);

                            // OPEN RTP CHANNEL
                            NettyChannelManager.getInstance().openRtpChannel(rtspUnitId, listenIp, rtpPort);
                            // TODO OPEN RTCP CHANNEL

                            RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnitId);
                            if (rtspNettyChannel != null) {
                                rtspNettyChannel.sendSetup(rtspUnit);
                            } else {
                                logger.warn("({}) ({}) Rtsp Channel is closed. Fail to process SETUP.", name, rtspUnitId);
                                rtspStateHandler.fire(
                                        RtspEvent.IDLE,
                                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                );
                            }
                        } else {
                            logger.warn("({}) ({}) () Fail to process DESCRIBE. Fail to parse the sdp.", name, rtspUnit.getRtspUnitId());
                            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                            rtspStateHandler.fire(
                                    RtspEvent.IDLE,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                    } else {
                        logger.warn("({}) ({}) () Fail to process DESCRIBE. Sdp is null.", name, rtspUnit.getRtspUnitId());
                        NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                        rtspStateHandler.fire(
                                RtspEvent.IDLE,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );
                    }
                }
            } else if (msg instanceof DefaultHttpResponse) { // RTSP Messages
                DefaultHttpResponse res = (DefaultHttpResponse) msg;

                // 1) OPTIONS
                switch (curState) {
                    case RtspState.OPTIONS:
                        logger.debug("({}) ({}) () < OPTIONS {}", name, rtspUnit.getRtspUnitId(), res);

                        if (res.status().code() == 200) {
                            RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnitId);
                            if (rtspNettyChannel != null) {
                                rtspNettyChannel.sendDescribe(rtspUnit);
                            } else {
                                logger.warn("({}) ({}) Rtsp Channel is closed. Fail to process DESCRIBE.", name, rtspUnitId);
                                rtspStateHandler.fire(
                                        RtspEvent.IDLE,
                                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                );
                            }
                        } else {
                            logger.warn("({}) ({}) () Fail to process OPTIONS. Status is not 200.", name, rtspUnit.getRtspUnitId());
                            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                            rtspStateHandler.fire(
                                    RtspEvent.IDLE,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                        break;
                    // 2) DESCRIBE
                    case RtspState.DESCRIBE:
                        logger.debug("({}) ({}) () < DESCRIBE {}", name, rtspUnit.getRtspUnitId(), res);

                        if (res.status().code() == 200) {
                            rtspStateHandler.fire(
                                    RtspEvent.DESCRIBE_OK,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        } else {
                            logger.warn("({}) ({}) () Fail to process DESCRIBE. Status code is not 200.", name, rtspUnit.getRtspUnitId());
                            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                            rtspStateHandler.fire(
                                    RtspEvent.IDLE,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                        break;
                    // 3) SETUP
                    case RtspState.SETUP:
                        logger.debug("({}) ({}) () < SETUP {}", name, rtspUnit.getRtspUnitId(), res);

                        String transportValue = res.headers().get("Transport");

                        // SSRC Parsing
                        int ssrcIndex = transportValue.indexOf("ssrc");
                        if (ssrcIndex >= 0) {
                            String ssrc;
                            if (transportValue.charAt(transportValue.length() - 1) == ';') { // 뒤에 값이 더 있는 경우
                                ssrc = transportValue.substring(ssrcIndex + 5, transportValue.lastIndexOf(';'));
                            } else { // ssrc 가 마지막인 경우
                                ssrc = transportValue.substring(ssrcIndex + 5);
                            }
                            rtspUnit.setSsrc(ssrc);
                            logger.debug("({}) ({}) SSRC: {}", name, rtspUnit.getRtspUnitId(), ssrc);
                        }

                        logger.debug("({}) ({}) Waiting to play...", name, rtspUnit.getRtspUnitId());
                    /*RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnitId);
                    if (rtspNettyChannel != null) {
                        rtspNettyChannel.sendPlay(rtspUnit,
                                rtspUnit.getStartTime(),
                                rtspUnit.getEndTime()
                        );
                    } else {
                        logger.warn("({}) ({}) Rtsp Channel is closed. Fail to process PLAY.", name, rtspUnitId);
                        rtspStateHandler.fire(
                                RtspEvent.IDLE,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );
                    }*/
                        break;
                    // 4) PLAY
                    case RtspState.PLAY:
                        logger.debug("({}) ({}) () < PLAY {}", name, rtspUnit.getRtspUnitId(), res);

                        if (res.status().code() == 200) {
                            String range = res.headers().get(RtspHeaderNames.RANGE);
                            logger.debug("range: {}", range);
                            String startTime = range.substring(range.indexOf("npt") + 4, range.indexOf("-"));
                            logger.debug("startTime: {}", startTime);
                            String endTime = null;
                            if (range.charAt(range.length() - 1) != '-') { // end time
                                endTime = range.substring(range.indexOf("-") + 1);
                                logger.debug("endTime: {}", endTime);
                            }
                            rtspUnit.setStartTime(Double.parseDouble(startTime));
                            if (endTime != null) {
                                rtspUnit.setEndTime(Double.parseDouble(endTime));
                            }

                            logger.debug("({}) ({}) () Success to process PLAY.", name, rtspUnit.getRtspUnitId());
                        } else {
                            logger.warn("({}) ({}) () Fail to process PLAY. Status code is not 200.", name, rtspUnit.getRtspUnitId());
                            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                            rtspStateHandler.fire(
                                    RtspEvent.IDLE,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                        break;
                    case RtspState.PAUSE:
                        logger.debug("({}) ({}) () < PAUSE {}", name, rtspUnit.getRtspUnitId(), res);

                        break;
                    case RtspState.STOP:
                        logger.debug("({}) ({}) () < TEARDOWN {}", name, rtspUnit.getRtspUnitId(), res);
                        NettyChannelManager.getInstance().deleteRtpChannel(rtspUnitId);
                        NettyChannelManager.getInstance().deleteRtcpChannel(rtspUnitId);
                        NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                        rtspStateHandler.fire(
                                RtspEvent.IDLE,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );
                        rtspUnit.clear();
                        break;
                    default:
                        logger.debug("({}) ({}) () < MSG {}", name, rtspUnit.getRtspUnitId(), res);
                        break;
                }
            }
        } catch (Exception e) {
            logger.warn("({}) ({}) Fail to handle UDP Packet.", name, e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.warn("({}) RtspChannelHandler is inactive.", name);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("({}) RtspChannelHandler.Exception (cause={})", name, cause.toString());
    }

    /*@Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }*/

    public String getName() {
        return name;
    }

    public String getRtspUnitId() {
        return rtspUnitId;
    }

    public String getListenIp() {
        return listenIp;
    }

    public int getListenRtspPort() {
        return listenRtspPort;
    }
}