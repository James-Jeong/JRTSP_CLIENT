package com.rtsp.client.media.netty.module;

import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.media.netty.handler.RtpChannelHandler;
import com.rtsp.client.service.AppInstance;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class RtpNettyChannel {

    private static final Logger log = LoggerFactory.getLogger(RtpNettyChannel.class);

    private final String rtspUnitId;
    private final String ip;
    private final int port;

    private Channel channel = null;
    private Bootstrap bootstrap;

    ////////////////////////////////////////////////////////////////////////////////

    public RtpNettyChannel(String rtspUnitId, String ip, int port) {
        this.rtspUnitId = rtspUnitId;
        this.ip = ip;
        this.port = port;
    }

    public void run () {
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();

        bootstrap = new Bootstrap();
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(configManager.getStreamThreadPoolSize());
        bootstrap.group(eventLoopGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .option(ChannelOption.SO_SNDBUF, configManager.getSendBufSize())
                .option(ChannelOption.SO_RCVBUF, configManager.getRecvBufSize())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    public void initChannel (final NioDatagramChannel ch) {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new RtpChannelHandler(rtspUnitId, ip, port));
                    }
                });
    }

    public Channel start() {
        if (channel != null) {
            return null;
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            log.warn("UnknownHostException is occurred. (ip={})", ip, e);
            return null;
        }

        try {
            ChannelFuture channelFuture = bootstrap.bind(address, port).sync();
            if (channelFuture == null) {
                log.warn("[{}] Fail to start the rtp channel. (ip={}, port={})", rtspUnitId, ip, port);
                return null;
            }

            channel = channelFuture.channel();
            log.debug("[{}] Success to start the rtp channel. (ip={}, port={})", rtspUnitId, ip, port);
            return channel;
        } catch (Exception e) {
            log.warn("[{}] Fail to start the rtp channel. (ip={}, port={})", rtspUnitId, ip, port, e);
            return null;
        }
    }

    public void stop() {
        if (channel == null) {
            log.warn("[{}] Fail to stop the rtp channel. (ip={}, port={})", rtspUnitId, ip, port);
            return;
        }

        channel.close();
        channel = null;
        log.debug("[{}] Success to stop the rtp channel. (ip={}, port={})", rtspUnitId, ip, port);
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

    public Channel getChannel() {
        return channel;
    }
}
