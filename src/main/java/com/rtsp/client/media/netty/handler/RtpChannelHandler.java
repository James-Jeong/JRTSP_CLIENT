package com.rtsp.client.media.netty.handler;

import com.fsm.module.StateHandler;
import com.rtsp.client.fsm.RtspEvent;
import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.protocol.RtpPacket;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

public class RtpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger log = LoggerFactory.getLogger(RtpChannelHandler.class);

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

            // TODO M3U8 먼저 도착(UDP, paylaod 가변), 그리고 TS 파일 수신함(RTP, payload 188)
            if (data.length < 188) { // M3U8
                log.debug("({}) ({}) >> Recv M3U8\n{}(size={})",
                        rtspUnitId, rtspUnit.getSessionId(),
                        new String(data, StandardCharsets.UTF_8), data.length
                );

                // 1) M3U8 데이터를 파일로 저장
                /*File m3u8File = new File(""); // TODO PATH

                // 2) 파일에서 데이터 읽어서 플레이리스트 생성
                List<MediaSegment> mediaSegmentList;
                MediaPlaylistParser parser = new MediaPlaylistParser();
                MediaPlaylist playlist = parser.readPlaylist(Paths.get(m3u8File.getAbsolutePath()));
                if (playlist != null) {
                    mediaSegmentList = playlist.mediaSegments();
                    RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
                    if (rtspUnit == null) {
                        log.warn("({}) RtpChannelHandler > RtspUnit is null... Fail to get the m3u8 data.", rtspUnitId);
                        return;
                    }

                    log.debug("({}) mediaSegmentList: {}", rtspUnit.getRtspUnitId(), mediaSegmentList);
                    StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
                    rtspStateHandler.fire(
                            RtspEvent.PLAY,
                            rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                    );
                }*/
            } else { // TS
                // TODO Video Streaming > MP4 Data (From TS File)
                RtpPacket rtpPacket = new RtpPacket(data, readBytes);
                byte[] payload = rtpPacket.getPayload();

                log.trace("({}) ({}) >> Recv TS RTP [{}] (payloadSize={})",
                        rtspUnit.getRtspUnitId(), rtspUnit.getSessionId(), rtpPacket, payload.length
                );
            }
        } catch (Exception e) {
            log.warn("RtpChannelHandler.channelRead0.Exception", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

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
