package com.rtsp.client.media.netty;

import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.media.netty.module.RtcpNettyChannel;
import com.rtsp.client.media.netty.module.RtpNettyChannel;
import com.rtsp.client.media.netty.module.RtspNettyChannel;
import com.rtsp.client.media.netty.module.RtspRegisterNettyChannel;
import com.rtsp.client.service.AppInstance;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class NettyChannelManager {

    private static final Logger log = LoggerFactory.getLogger(NettyChannelManager.class);

    private static NettyChannelManager nettyChannelManager = null;

    private RtspRegisterNettyChannel rtspRegisterNettyChannel = null;

    private final HashMap<String, RtspNettyChannel> rtspChannelMap = new HashMap<>();
    private final ReentrantLock rtspChannelMapLock = new ReentrantLock();

    private final HashMap<String, RtpNettyChannel> rtpChannelMap = new HashMap<>();
    private final ReentrantLock rtpChannelMapLock = new ReentrantLock();

    private final HashMap<String, RtcpNettyChannel> rtcpChannelMap = new HashMap<>();
    private final ReentrantLock rtcpChannelMapLock = new ReentrantLock();

    ////////////////////////////////////////////////////////////////////////////////

    public NettyChannelManager() {
        // nothing
    }

    public static NettyChannelManager getInstance() {
        if (nettyChannelManager == null) {
            nettyChannelManager = new NettyChannelManager();
        }
        return nettyChannelManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    // Register 버튼 클릭 시 호출
    public void addRegisterChannel() {
        if (rtspRegisterNettyChannel != null) {
            return;
        }

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        rtspRegisterNettyChannel = new RtspRegisterNettyChannel(
                configManager.getLocalListenIp(),
                configManager.getLocalListenPort()
        );
        rtspRegisterNettyChannel.run();
        rtspRegisterNettyChannel.connect(configManager.getTargetIp(), configManager.getTargetPort());
    }

    // 프로그램 종료 시 호출
    public void removeRegisterChannel() {
        if (rtspRegisterNettyChannel == null) {
            return;
        }

        rtspRegisterNettyChannel.stop();
        rtspRegisterNettyChannel = null;
    }

    public RtspRegisterNettyChannel getRegisterChannel() {
        return rtspRegisterNettyChannel;
    }

    ////////////////////////////////////////////////////////////////////////////////

    // Register 200 OK 응답 호출 시 생성되는 Rtsp Response 수신 채널
    public RtspNettyChannel openRtspChannel(String rtspUnitId, String ip, int port) {
        try {
            rtspChannelMapLock.lock();

            if (rtspChannelMap.get(rtspUnitId) != null) {
                log.trace("| ({}) Fail to add the rtsp channel. Key is duplicated.", rtspUnitId);
                return null;
            }

            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            String localListenIp = configManager.getLocalListenIp();
            int localListenPort = configManager.getLocalRtspPort();
            RtspNettyChannel rtspNettyChannel = new RtspNettyChannel(rtspUnitId, localListenIp, localListenPort);
            rtspNettyChannel.run();

            // 메시지 송신용 채널 open
            Channel channel = rtspNettyChannel.openChannel(
                    ip,
                    port
            );

            if (channel == null) {
                rtspNettyChannel.closeChannel();
                rtspNettyChannel.stop();
                log.warn("| ({}) Fail to add the rtsp channel.", rtspUnitId);
                return null;
            }

            rtspChannelMap.putIfAbsent(rtspUnitId, rtspNettyChannel);
            log.debug("| ({}) Success to add rtsp channel.", rtspUnitId);
            return rtspNettyChannel;
        } catch (Exception e) {
            log.warn("| ({}) Fail to add rtsp channel (ip={}, port={}).", rtspUnitId, ip, port, e);
            return null;
        } finally {
            rtspChannelMapLock.unlock();
        }
    }

    public void deleteRtspChannel(String rtspUnitId) {
        try {
            rtspChannelMapLock.lock();

            if (!rtspChannelMap.isEmpty()) {
                RtspNettyChannel rtspNettyChannel = rtspChannelMap.get(rtspUnitId);
                if (rtspNettyChannel == null) {
                    return;
                }

                rtspNettyChannel.closeChannel();
                rtspNettyChannel.stop();
                rtspChannelMap.remove(rtspUnitId);

                log.debug("| ({}) Success to close the rtsp channel.", rtspUnitId);
            }
        } catch (Exception e) {
            log.warn("| ({}) Fail to close the rtsp channel.", rtspUnitId, e);
        } finally {
            rtspChannelMapLock.unlock();
        }
    }

    public void deleteAllRtspChannels () {
        try {
            rtspChannelMapLock.lock();

            if (!rtspChannelMap.isEmpty()) {
                for (Map.Entry<String, RtspNettyChannel> entry : rtspChannelMap.entrySet()) {
                    RtspNettyChannel rtspNettyChannel = entry.getValue();
                    if (rtspNettyChannel == null) {
                        continue;
                    }

                    rtspNettyChannel.closeChannel();
                    rtspNettyChannel.stop();
                    rtspChannelMap.remove(entry.getKey());
                }

                log.debug("| Success to close all rtsp channel(s).");
            }
        } catch (Exception e) {
            log.warn("| Fail to close all rtsp channel(s).", e);
        } finally {
            rtspChannelMapLock.unlock();
        }
    }

    public RtspNettyChannel getRtspChannel(String rtspUnitId) {
        try {
            rtspChannelMapLock.lock();

            return rtspChannelMap.get(rtspUnitId);
        } catch (Exception e) {
            return null;
        } finally {
            rtspChannelMapLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void openRtpChannel(String rtspUnitId, String ip, int port) {
        try {
            rtpChannelMapLock.lock();

            if (rtpChannelMap.get(rtspUnitId) != null) {
                log.trace("| ({}) Fail to add the rtp channel. Key is duplicated.", rtspUnitId);
                return;
            }

            RtpNettyChannel rtpNettyChannel = new RtpNettyChannel(rtspUnitId, ip, port);
            rtpNettyChannel.run();

            Channel channel = rtpNettyChannel.start();
            if (channel == null) {
                log.warn("| ({}) Fail to add the rtp channel.", rtspUnitId);
                return;
            }

            rtpChannelMap.putIfAbsent(rtspUnitId, rtpNettyChannel);
            log.debug("| ({}) Success to add rtp channel.", rtspUnitId);
        } catch (Exception e) {
            log.warn("| ({}) Fail to add rtp channel (ip={}, port={}).", rtspUnitId, ip, port, e);
        } finally {
            rtpChannelMapLock.unlock();
        }
    }

    public void deleteRtpChannel(String rtspUnitId) {
        try {
            rtpChannelMapLock.lock();

            if (!rtpChannelMap.isEmpty()) {
                RtpNettyChannel rtpNettyChannel = rtpChannelMap.get(rtspUnitId);
                if (rtpNettyChannel == null) {
                    return;
                }

                rtpNettyChannel.stop();
                rtpChannelMap.remove(rtspUnitId);

                log.debug("| ({}) Success to close the rtp channel.", rtspUnitId);
            }
        } catch (Exception e) {
            log.warn("| ({}) Fail to close the rtp channel.", rtspUnitId, e);
        } finally {
            rtpChannelMapLock.unlock();
        }
    }

    public void deleteAllRtpChannels () {
        try {
            rtpChannelMapLock.lock();

            if (!rtspChannelMap.isEmpty()) {
                for (Map.Entry<String, RtpNettyChannel> entry : rtpChannelMap.entrySet()) {
                    RtpNettyChannel rtpNettyChannel = entry.getValue();
                    if (rtpNettyChannel == null) {
                        continue;
                    }

                    rtpNettyChannel.stop();
                    rtpChannelMap.remove(entry.getKey());
                }

                log.debug("| Success to close all rtp channel(s).");
            }
        } catch (Exception e) {
            log.warn("| Fail to close all rtp channel(s).", e);
        } finally {
            rtpChannelMapLock.unlock();
        }
    }

    public RtpNettyChannel getRtpChannel(String rtspUnitId) {
        try {
            rtpChannelMapLock.lock();

            return rtpChannelMap.get(rtspUnitId);
        } catch (Exception e) {
            return null;
        } finally {
            rtpChannelMapLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public RtcpNettyChannel openRtcpChannel(String rtspUnitId, String ip, int port) {
        try {
            rtcpChannelMapLock.lock();

            if (rtcpChannelMap.get(rtspUnitId) != null) {
                log.trace("| ({}) Fail to add the rtcp channel. Key is duplicated.", rtspUnitId);
                return null;
            }

            /*int port = ResourceManager.getInstance().takePort();
            if (port == -1) {
                log.warn("| Fail to add the channel. Port is full. (key={})", key);
                return false;
            }*/

            RtcpNettyChannel rtcpNettyChannel = new RtcpNettyChannel(rtspUnitId, ip, port);
            rtcpNettyChannel.run(ip, port);

            // 메시지 수신용 채널 open
            Channel channel = rtcpNettyChannel.openChannel(
                    ip,
                    port
            );

            if (channel == null) {
                rtcpNettyChannel.closeChannel();
                rtcpNettyChannel.stop();
                log.warn("| ({}) Fail to add the rtcp channel.", rtspUnitId);
                return null;
            }

            rtcpChannelMap.putIfAbsent(rtspUnitId, rtcpNettyChannel);
            log.debug("| ({}) Success to add rtcp channel.", rtspUnitId);
            return rtcpNettyChannel;
        } catch (Exception e) {
            log.warn("| ({}) Fail to add rtcp channel (ip={}, port={}).", rtspUnitId, ip, port, e);
            return null;
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    public void deleteRtcpChannel(String rtspUnitId) {
        try {
            rtcpChannelMapLock.lock();

            if (!rtcpChannelMap.isEmpty()) {
                RtcpNettyChannel rtcpNettyChannel = rtcpChannelMap.get(rtspUnitId);
                if (rtcpNettyChannel == null) {
                    return;
                }

                rtcpNettyChannel.closeChannel();
                rtcpNettyChannel.stop();
                rtcpChannelMap.remove(rtspUnitId);

                log.debug("| ({}) Success to close the rtcp channel.", rtspUnitId);
            }
        } catch (Exception e) {
            log.warn("| ({}) Fail to close the rtcp channel.", rtspUnitId, e);
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    public void deleteAllRtcpChannels () {
        try {
            rtcpChannelMapLock.lock();

            if (!rtcpChannelMap.isEmpty()) {
                for (Map.Entry<String, RtcpNettyChannel> entry : rtcpChannelMap.entrySet()) {
                    RtcpNettyChannel rtcpNettyChannel = entry.getValue();
                    if (rtcpNettyChannel == null) {
                        continue;
                    }

                    rtcpNettyChannel.closeChannel();
                    rtcpNettyChannel.stop();
                    rtcpChannelMap.remove(entry.getKey());
                }

                log.debug("| Success to close all rtcp channel(s).");
            }
        } catch (Exception e) {
            log.warn("| Fail to close all rtcp channel(s).", e);
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    public RtcpNettyChannel getRtcpChannel(String rtspUnitId) {
        try {
            rtcpChannelMapLock.lock();

            return rtcpChannelMap.get(rtspUnitId);
        } catch (Exception e) {
            return null;
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }
    
}
