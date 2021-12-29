package com.rtsp.client.media.netty.module;

import com.fsm.module.StateHandler;
import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.fsm.RtspEvent;
import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.media.netty.handler.RtspChannelInboundHandler;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.service.AppInstance;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.rtsp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @class public class NettyChannel
 * @brief NettyChannel class
 */
public class RtspNettyChannel { // > TCP

    private static final Logger logger = LoggerFactory.getLogger(RtspNettyChannel.class);

    private Bootstrap b;
    private EventLoopGroup eventLoopGroup;

    /*메시지 수신용 채널 */
    private Channel channel;

    private final String rtspUnitId;
    private final String listenIp;
    private final int listenPort;

    private int seqNum = 1;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspNettyChannel(String rtspUnitId, String ip, int port) {
        this.rtspUnitId = rtspUnitId;
        this.listenIp = ip;
        this.listenPort = port;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void run () {
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();

        eventLoopGroup = new NioEventLoopGroup(configManager.getStreamThreadPoolSize());
        b = new Bootstrap();
        b.group(eventLoopGroup);
        b.channel(NioSocketChannel.class)
                .option(ChannelOption.SO_RCVBUF, configManager.getRecvBufSize())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        final ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new RtspDecoder(), new RtspEncoder());
                        pipeline.addLast(
                                new RtspChannelInboundHandler(
                                        rtspUnitId,
                                        listenIp,
                                        listenPort
                                )
                        );
                    }
                });
    }

    /**
     * @fn public void stop()
     * @brief Netty Channel 을 종료하는 함수
     */
    public void stop () {
        eventLoopGroup.shutdownGracefully();
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @param ip   바인딩할 ip
     * @param port 바인당할 port
     * @return 성공 시 생성된 Channel, 실패 시 null 반환
     * @fn public Channel openChannel(String ip, int port)
     * @brief Netty Server Channel 을 생성하는 함수
     */
    public Channel openChannel (String ip, int port) {
        if (channel != null) {
            logger.warn("Channel is already opened.");
            return null;
        }

        InetAddress address;
        ChannelFuture channelFuture;

        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            logger.warn("UnknownHostException is occurred. (ip={})", ip, e);
            return null;
        }

        try {
            channelFuture = b.connect(address, port).sync();
            channel = channelFuture.channel();
            //logger.debug("Channel is opened. (ip={}, port={})", address, port);

            return channelFuture.channel();
        } catch (Exception e) {
            logger.warn("Channel is interrupted. (address={}:{})", ip, port, e);
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    private void sendMessage(DefaultHttpRequest request) {
        closeChannel();
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        openChannel(
                configManager.getTargetRtspIp(),
                configManager.getTargetRtspPort()
        );

        if (channel != null) {
            channel.writeAndFlush(request);
            seqNum++;

            logger.debug("({}) Send the request. (\n{})", rtspUnitId, request);
        } else {
            logger.warn("({}) Channel is null. Fail to send the request. ({})", rtspUnitId, request);
        }
    }

    public void sendOptions(RtspUnit rtspUnit) {
        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
        rtspStateHandler.fire(
                RtspEvent.OPTIONS,
                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
        );

        DefaultHttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, HttpMethod.OPTIONS, rtspUnit.getUri());
        request.headers().set(RtspHeaderNames.CSEQ, String.valueOf(seqNum));
        request.headers().set(RtspHeaderNames.USER_AGENT, AppInstance.getInstance().getConfigManager().getUserAgent());
        request.headers().set(RtspHeaderNames.SESSION, String.valueOf(rtspUnit.getSessionId()));
        sendMessage(request);
    }

    public void sendDescribe(RtspUnit rtspUnit) {
        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
        rtspStateHandler.fire(
                RtspEvent.DESCRIBE,
                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
        );

        DefaultHttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.DESCRIBE, rtspUnit.getUri());
        request.headers().set(RtspHeaderNames.CSEQ, String.valueOf(seqNum));
        request.headers().set(RtspHeaderNames.USER_AGENT, AppInstance.getInstance().getConfigManager().getUserAgent());
        request.headers().set(RtspHeaderNames.ACCEPT, "application/sdp");
        sendMessage(request);
    }

    public void sendSetup(RtspUnit rtspUnit) {
        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
        rtspStateHandler.fire(
                RtspEvent.SETUP,
                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
        );

        DefaultHttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.SETUP, rtspUnit.getUri());
        request.headers().set(RtspHeaderNames.CSEQ, String.valueOf(seqNum));
        request.headers().set(RtspHeaderNames.USER_AGENT, AppInstance.getInstance().getConfigManager().getUserAgent());
        request.headers().set(RtspHeaderNames.TRANSPORT, "RTP/AVP;unicast;client_port=" + rtspUnit.getListenRtpPort() + "-" + rtspUnit.getListenRtcpPort());
        sendMessage(request);
    }

    public void sendPlay(RtspUnit rtspUnit, double startTime, double endTime) {
        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
        rtspStateHandler.fire(
                RtspEvent.PLAY,
                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
        );

        DefaultHttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.PLAY, rtspUnit.getUri());
        request.headers().set(RtspHeaderNames.CSEQ, String.valueOf(seqNum));
        request.headers().set(RtspHeaderNames.USER_AGENT, AppInstance.getInstance().getConfigManager().getUserAgent());
        request.headers().set(RtspHeaderNames.SESSION, String.valueOf(rtspUnit.getSessionId()));
        if (endTime > 0) {
            request.headers().set(RtspHeaderNames.RANGE, "npt=" + String.format("%.3f", startTime) + "-" + String.format("%.3f", endTime));
        } else {
            request.headers().set(RtspHeaderNames.RANGE, "npt=" + String.format("%.3f", startTime) + "-");
        }
        sendMessage(request);
    }

    public void sendPause(RtspUnit rtspUnit) {
        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
        rtspStateHandler.fire(
                RtspEvent.PAUSE,
                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
        );

        DefaultHttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.PAUSE, rtspUnit.getUri());
        request.headers().set(RtspHeaderNames.CSEQ, String.valueOf(seqNum));
        request.headers().set(RtspHeaderNames.USER_AGENT, AppInstance.getInstance().getConfigManager().getUserAgent());
        request.headers().set(RtspHeaderNames.SESSION, String.valueOf(rtspUnit.getSessionId()));
        sendMessage(request);
    }

    public void sendStop(RtspUnit rtspUnit) {
        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
        rtspStateHandler.fire(
                RtspEvent.TEARDOWN,
                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
        );

        DefaultHttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.TEARDOWN, rtspUnit.getUri());
        request.headers().set(RtspHeaderNames.CSEQ, String.valueOf(seqNum));
        request.headers().set(RtspHeaderNames.USER_AGENT, AppInstance.getInstance().getConfigManager().getUserAgent());
        request.headers().set(RtspHeaderNames.SESSION, String.valueOf(rtspUnit.getSessionId()));
        sendMessage(request);
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public void closeChannel()
     * @brief Netty Server Channel 을 닫는 함수
     */
    public void closeChannel ( ) {
        if (channel == null) {
            logger.warn("Channel is already closed.");
            return;
        }

        channel.close();
        channel = null;
        seqNum = 1;
        //logger.debug("Channel is closed.");
    }

    public String getListenIp() {
        return listenIp;
    }

    public int getListenPort() {
        return listenPort;
    }

}
