package com.rtsp.client.media.netty.handler;

import com.fsm.module.StateHandler;
import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.fsm.RtspEvent;
import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.gui.component.panel.VideoControlPanel;
import com.rtsp.client.media.module.StreamReceiver;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.media.sdp.base.Sdp;
import com.rtsp.client.media.sdp.base.attribute.RtpAttribute;
import com.rtsp.client.service.AppInstance;
import com.rtsp.client.service.scheduler.schedule.ScheduleManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final StreamReceiver videoPlayJob;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspChannelInboundHandler(String rtspUnitId, String listenIp, int listenRtspPort) {
        this.name = "RTSP_" + rtspUnitId + "_" + listenIp + ":" + listenRtspPort;
        this.rtspUnitId = rtspUnitId;
        this.listenIp = listenIp;
        this.listenRtspPort = listenRtspPort;

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        ScheduleManager.getInstance().initJob(
                "VIDEO_PLAY",
                configManager.getStreamThreadPoolSize(),
                configManager.getStreamThreadPoolSize()
        );

        videoPlayJob = new StreamReceiver(
                StreamReceiver.class.getSimpleName() + "_" + rtspUnitId,
                1, 1, TimeUnit.MILLISECONDS,
                1, 1, true
        );

        //logger.debug("({}) RtspChannelHandler is created. (listenIp={}, listenRtspPort={})", name, listenIp, listenRtspPort);
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
                                RtspEvent.DESCRIBE_FAIL,
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
                            Sdp remoteSdp = rtspUnit.getSdp();
                            logger.debug("({}) ({}) REMOTE SDP: {}", name, rtspUnitId, remoteSdp.getData(true));

                            int rtpPort = remoteSdp.getMediaPort(Sdp.VIDEO);
                            rtspUnit.setListenRtpPort(rtpPort);
                            rtspUnit.setListenRtcpPort(rtpPort + 1);

                            Sdp localSdp = AppInstance.getInstance().getConfigManager().loadLocalSdpConfig(
                                    rtspUnitId,
                                    rtspUnit.getListenRtpPort()
                            );
                            logger.debug("({}) ({}) LOCAL SDP: {}", name, rtspUnitId, localSdp.getData(true));

                            boolean isCodecMatched = false;
                            List<RtpAttribute> localCodecList = localSdp.getMediaDescriptionFactory().getMediaFactory(Sdp.VIDEO).getCodecList();
                            List<RtpAttribute> remoteCodecList = remoteSdp.getMediaDescriptionFactory().getMediaFactory(Sdp.VIDEO).getCodecList();
                            String localCodecName = null, remoteCodecName = null;
                            for (RtpAttribute localRtpAttribute : localCodecList) {
                                localCodecName = localRtpAttribute.getRtpMapAttributeFactory().getCodecName();
                                if (localCodecName == null) { continue; }
                                for (RtpAttribute remoteRtpAttribute : remoteCodecList) {
                                    remoteCodecName = remoteRtpAttribute.getRtpMapAttributeFactory().getCodecName();
                                    if (localCodecName.equals(remoteCodecName)) {
                                        logger.debug("({}) ({}) CODEC MATCHED! (local={}, remote={})", name, rtspUnitId, localCodecName, remoteCodecName);
                                        isCodecMatched = true;
                                        break;
                                    }
                                }
                            }

                            if (!isCodecMatched) {
                                logger.warn("({}) ({}) () Fail to process DESCRIBE. Fail to negotiate the sdp. (local={}, remote={})", name, rtspUnit.getRtspUnitId(), localCodecName, remoteCodecName);
                                // Send TEARDOWN
                                RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnitId);
                                if (rtspNettyChannel != null) {
                                    rtspNettyChannel.sendStop(rtspUnit);
                                }
                                return;
                            }

                            // OPEN RTP CHANNEL
                            NettyChannelManager.getInstance().openRtpChannel(rtspUnitId, listenIp, rtpPort);
                            // TODO : OPEN RTCP CHANNEL

                            RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnitId);
                            if (rtspNettyChannel != null) {
                                rtspNettyChannel.sendSetup(rtspUnit);
                            } else {
                                logger.warn("({}) ({}) Rtsp Channel is closed. Fail to process SETUP.", name, rtspUnitId);
                                rtspStateHandler.fire(
                                        RtspEvent.DESCRIBE_FAIL,
                                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                );
                            }
                        } else {
                            logger.warn("({}) ({}) () Fail to process DESCRIBE. Fail to parse the sdp.", name, rtspUnit.getRtspUnitId());
                            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                            rtspStateHandler.fire(
                                    RtspEvent.DESCRIBE_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                    } else {
                        logger.warn("({}) ({}) () Fail to process DESCRIBE. Sdp is null.", name, rtspUnit.getRtspUnitId());
                        NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                        rtspStateHandler.fire(
                                RtspEvent.DESCRIBE_FAIL,
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

                        if (res.status().code() == HttpResponseStatus.OK.code()) {
                            RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnitId);
                            if (rtspNettyChannel != null) {
                                rtspNettyChannel.sendDescribe(rtspUnit);
                            } else {
                                logger.warn("({}) ({}) Rtsp Channel is closed. Fail to process DESCRIBE.", name, rtspUnitId);
                                rtspStateHandler.fire(
                                        RtspEvent.OPTIONS_FAIL,
                                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                );
                            }
                        } else {
                            logger.warn("({}) ({}) () Fail to process OPTIONS. Status code is not HttpResponseStatus.OK.code(). (code={})", name, rtspUnit.getRtspUnitId(), res.status().code());
                            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                            rtspStateHandler.fire(
                                    RtspEvent.OPTIONS_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                        break;
                    // 2) DESCRIBE
                    case RtspState.DESCRIBE:
                        logger.debug("({}) ({}) () < DESCRIBE {}", name, rtspUnit.getRtspUnitId(), res);

                        if (res.status().code() == HttpResponseStatus.OK.code()) {
                            rtspStateHandler.fire(
                                    RtspEvent.DESCRIBE_OK,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        } else {
                            logger.warn("({}) ({}) () Fail to process DESCRIBE. Status code is not HttpResponseStatus.OK.code(). (code={})", name, rtspUnit.getRtspUnitId(), res.status().code());
                            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                            rtspStateHandler.fire(
                                    RtspEvent.DESCRIBE_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                        break;
                    // 3) SETUP
                    case RtspState.SETUP:
                        logger.debug("({}) ({}) () < SETUP {}", name, rtspUnit.getRtspUnitId(), res);

                        if (res.status().code() == HttpResponseStatus.OK.code()) {
                            String transportValue = res.headers().get(RtspHeaderNames.TRANSPORT);
                            if (transportValue != null) {
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

                                RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnit.getRtspUnitId());
                                if (rtspNettyChannel != null) {
                                    rtspNettyChannel.sendPlay(rtspUnit,
                                            rtspUnit.getStartTime(),
                                            rtspUnit.getEndTime()
                                    );
                                } else {
                                    logger.warn("({}) Rtsp Channel is closed. Fail to process PLAY.", rtspUnit.getRtspUnitId());
                                    rtspStateHandler.fire(
                                            RtspEvent.SETUP_FAIL,
                                            rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                    );
                                }
                            }
                        } else {
                            logger.warn("({}) ({}) () Fail to process SETUP. Status code is not HttpResponseStatus.OK.code(). (code={})", name, rtspUnit.getRtspUnitId(), res.status().code());
                            rtspStateHandler.fire(
                                    RtspEvent.SETUP_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                        break;
                    // 4) PLAY
                    case RtspState.PLAY:
                        logger.debug("({}) ({}) () < PLAY {}", name, rtspUnit.getRtspUnitId(), res);

                        if (res.status().code() == HttpResponseStatus.OK.code()) {
                            rtspUnit.setPaused(false);

                            String range = res.headers().get(RtspHeaderNames.RANGE);
                            logger.debug("({}) ({}) Range: {}", name, rtspUnit.getRtspUnitId(), range);
                            String startTime = range.substring(range.indexOf("npt") + 4, range.indexOf("-"));
                            logger.debug("({}) ({}) StartTime: {}", name, rtspUnit.getRtspUnitId(), startTime);
                            String endTime = null;
                            if (range.charAt(range.length() - 1) != '-') { // end time
                                endTime = range.substring(range.indexOf("-") + 1);
                                logger.debug("({}) ({}) EndTime: {}", name, rtspUnit.getRtspUnitId(), endTime);
                            }
                            rtspUnit.setStartTime(Double.parseDouble(startTime));
                            if (endTime != null) {
                                rtspUnit.setEndTime(Double.parseDouble(endTime));
                            }

                            GuiManager.getInstance().getControlPanel().applyPlayButtonStatus();
                            ScheduleManager.getInstance().startJob(RtspUnit.VIDEO_JOB_KEY, videoPlayJob);

                            logger.debug("({}) ({}) () Success to process PLAY.", name, rtspUnit.getRtspUnitId());
                        } else {
                            logger.warn("({}) ({}) () Fail to process PLAY. Status code is not HttpResponseStatus.OK.code(). (code={})", name, rtspUnit.getRtspUnitId(), res.status().code());
                            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
                            rtspStateHandler.fire(
                                    RtspEvent.IDLE,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                        break;
                    case RtspState.PAUSE:
                        logger.debug("({}) ({}) () < PAUSE {}", name, rtspUnit.getRtspUnitId(), res);
                        if (res.status().code() == HttpResponseStatus.OK.code()) {
                            rtspUnit.setPaused(true);

                            MediaPlayer mediaPlayer = GuiManager.getInstance().getVideoPanel().getMediaPlayer();
                            if (mediaPlayer != null) {
                                mediaPlayer.pause();
                            }

                            GuiManager.getInstance().getControlPanel().applyPauseButtonStatus();
                            RtspManager.getInstance().clearRtspUnit(false, false);
                        } else {
                            logger.warn("({}) ({}) () Fail to process PAUSE. Status code is not HttpResponseStatus.OK.code(). (code={})", name, rtspUnit.getRtspUnitId(), res.status().code());
                            rtspStateHandler.fire(
                                    RtspEvent.PAUSE_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );

                            // PAUSE 실패 > 이미 동영상 스트리밍 종료되었다는 의미 > TEARDOWN 상태로 전환 필요
                            rtspUnit.setPaused(false);

                            // Send TEARDOWN
                            RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnit.getRtspUnitId());
                            if (rtspNettyChannel != null) {
                                rtspNettyChannel.sendStop(rtspUnit);
                            }
                        }
                        break;
                    case RtspState.STOP:
                        logger.debug("({}) ({}) () < TEARDOWN {}", name, rtspUnit.getRtspUnitId(), res);
                        if (res.status().code() == HttpResponseStatus.OK.code()) {
                            VideoControlPanel videoControlPanel = GuiManager.getInstance().getVideoControlPanel();
                            videoControlPanel.setVideoProgressBar(0.0);
                            videoControlPanel.setVideoStatus(0, 0);

                            MediaPlayer mediaPlayer = GuiManager.getInstance().getVideoPanel().getMediaPlayer();
                            if (mediaPlayer != null) {
                                mediaPlayer.seek(new Duration(0));
                                mediaPlayer.stop();
                                mediaPlayer.dispose();
                                GuiManager.getInstance().getVideoPanel().initMediaView();
                            }

                            ScheduleManager.getInstance().stopJob(RtspUnit.VIDEO_JOB_KEY, videoPlayJob);

                            GuiManager.getInstance().getControlPanel().applyStopButtonStatus();
                            RtspManager.getInstance().clearRtspUnit(true, false);
                        } else {
                            logger.warn("({}) ({}) () Fail to process STOP. Status code is not HttpResponseStatus.OK.code(). (code={})", name, rtspUnit.getRtspUnitId(), res.status().code());
                            rtspStateHandler.fire(
                                    RtspEvent.TEARDOWN_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                        break;
                    default:
                        logger.debug("({}) ({}) () < MSG (curState={}) {}", name, rtspUnit.getRtspUnitId(), curState, res);
                        break;
                }
            }
        } catch (Exception e) {
            logger.warn("({}) ({}) Fail to handle UDP Packet.", name, rtspUnitId, e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        //logger.warn("({}) RtspChannelHandler is inactive.", name);
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
