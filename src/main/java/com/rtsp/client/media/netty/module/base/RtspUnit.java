package com.rtsp.client.media.netty.module.base;

import com.fsm.StateManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.file.FileManager;
import com.rtsp.client.fsm.RtspFsmManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspNettyChannel;
import com.rtsp.client.media.sdp.SdpParser;
import com.rtsp.client.media.sdp.base.Sdp;
import com.rtsp.client.service.AppInstance;
import com.rtsp.client.service.base.ConcurrentCyclicFIFO;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;

/**
 * @class public class RtspUnit
 * @brief RtspUnit class
 */
public class RtspUnit {

    private static final Logger logger = LoggerFactory.getLogger(RtspUnit.class);

    public static final int MAX_SESSION_ID = 100000;

    private final String uri;
    private final String rtspUnitId; // ID of the RTSP session
    private long sessionId; // ID of the session
    private final SdpParser sdpParser = new SdpParser();

    private int congestionLevel = 0;

    private RtspNettyChannel rtspChannel;
    private final String targetIp;
    private final int targetPort;
    private String ssrc = null;

    private final RtspFsmManager rtspFsmManager = new RtspFsmManager();
    private final String rtspStateUnitId;

    private int listenRtpPort = 0;
    private int listenRtcpPort = 0;
    private String transportType = null;

    private Sdp sdp = null;

    private Double startTime = 0.000;
    private Double endTime = 0.000;

    private final StopWatch rtpTimeoutStopWatch = new StopWatch();
    private boolean isStarted = false;
    private boolean isPaused = false;
    private boolean isCompleted = false;
    private boolean isRegistered = false;
    private boolean isRecvData = false;

    private final ConcurrentCyclicFIFO<byte[]> readBuffer = new ConcurrentCyclicFIFO<>();

    private final FileManager fileManager;
    private final String fileNameOnly;
    private final String tempFileRootPath;
    private final String m3u8FilePath;
    private final String tsFilePath;
    private final String mp4FilePath;
    private int tsFileLimit = 0;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspUnit(String targetIp, int targetPort, String uri) {
        this.uri = uri;
        this.rtspUnitId = UUID.randomUUID().toString();
        this.rtspStateUnitId = UUID.randomUUID().toString();
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.fileManager = new FileManager(rtspUnitId);
        rtspFsmManager.init(this);

        fileNameOnly = fileManager.getFileNameExceptForExtension(uri);

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        this.tempFileRootPath = configManager.getTempRootPath() + fileNameOnly + "_tmp";
        File tempRootPathDirectory = new File(tempFileRootPath);
        if (!tempRootPathDirectory.exists()) {
            if (tempRootPathDirectory.mkdirs()) {
                logger.debug("({}) New temp root directory is created. ({})", rtspUnitId, tempFileRootPath);
            }
        }

        this.m3u8FilePath = tempFileRootPath + fileNameOnly + ".m3u8";
        this.tsFilePath = tempFileRootPath + fileNameOnly + "%d.ts";
        this.mp4FilePath = tempFileRootPath + fileNameOnly + ".mp4";;

        logger.debug("({}) RtspUnit is created. (m3u8FilePath={}, tsFilePath={})", rtspUnitId, m3u8FilePath, tsFilePath);
    }

    public void open() {
        rtspChannel = NettyChannelManager.getInstance().openRtspChannel(
                rtspUnitId,
                targetIp,
                targetPort,
                uri
        );
    }

    ////////////////////////////////////////////////////////////////////////////////

    public boolean isRecvData() {
        return isRecvData;
    }

    public void setRecvData(boolean recvData) {
        isRecvData = recvData;
    }

    public StopWatch getRtpTimeoutStopWatch() {
        return rtpTimeoutStopWatch;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public String getUri() {
        return uri;
    }

    public String getMp4FilePath() {
        return mp4FilePath;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public int getTsFileLimit() {
        return tsFileLimit;
    }

    public void setTsFileLimit(int tsFileLimit) {
        this.tsFileLimit = tsFileLimit;
    }

    public String getFileNameOnly() {
        return fileNameOnly;
    }

    public String getTempFileRootPath() {
        return tempFileRootPath;
    }

    public String getM3u8FilePath() {
        return m3u8FilePath;
    }

    public String getTsFilePath() {
        return tsFilePath;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public Double getStartTime() {
        return startTime;
    }

    public void setStartTime(Double startTime) {
        this.startTime = startTime;
        logger.debug("({}) ({}) startTime: {}", rtspUnitId, sessionId, this.startTime);
    }

    public Double getEndTime() {
        return endTime;
    }

    public void setEndTime(Double endTime) {
        this.endTime = endTime;
        logger.debug("({}) ({}) endTime: {}", rtspUnitId, sessionId, this.endTime);
    }

    public String getSsrc() {
        return ssrc;
    }

    public void setSsrc(String ssrc) {
        this.ssrc = ssrc;
    }

    public Sdp getSdp() {
        return sdp;
    }

    public void setSdp(Sdp sdp) {
        this.sdp = sdp;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public StateManager getStateManager() {
        return rtspFsmManager.getStateManager();
    }

    public String getRtspStateUnitId() {
        return rtspStateUnitId;
    }

    public String getRtspUnitId() {
        return rtspUnitId;
    }

    public int getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(int congestionLevel) {
        this.congestionLevel = congestionLevel;
    }

    public RtspNettyChannel getRtspChannel() {
        return rtspChannel;
    }

    public int getListenRtpPort() {
        return listenRtpPort;
    }

    public void setListenRtpPort(int listenRtpPort) {
        this.listenRtpPort = listenRtpPort;
    }

    public int getListenRtcpPort() {
        return listenRtcpPort;
    }

    public void setListenRtcpPort(int listenRtcpPort) {
        this.listenRtcpPort = listenRtcpPort;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public boolean parseSdp(String sdpStr) {
        try {
            Sdp sdp = sdpParser.parseSdp(
                    rtspUnitId, null, null, sdpStr
            );
            setSdp(sdp);
            return sdp != null;
        } catch (Exception e) {
            logger.warn("({}) Fail to parse the sdp. (sdp={})", rtspUnitId, sdpStr, e);
            return false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void clear(boolean isStopped) {
        if (isStopped) {
            sessionId = 0;
            congestionLevel = 0;
            rtspChannel = null;
            listenRtpPort = 0;
            listenRtcpPort = 0;
            transportType = null;
            sdp = null;
            startTime = 0.000;
            endTime = 0.000;
            isPaused = false;
            isCompleted = false;
        }

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        if (configManager.isDeleteM3u8()) {
            fileManager.removeM3U8File();
        }

        if (configManager.isDeleteTs()) {
            fileManager.removeAllTsFiles();
        }

        if (configManager.isDeleteMp4()) {
            File mp4File = new File(mp4FilePath);
            if (mp4File.exists()) {
                if (mp4File.delete()) {
                    logger.debug("({}) Success to delete the mp4 file. (path={})", rtspUnitId, mp4FilePath);
                }
            }
        }

        if (configManager.isDeleteM3u8() && configManager.isDeleteTs() && configManager.isDeleteMp4()) {
            File tempRootPathDirectory = new File(tempFileRootPath);
            if (tempRootPathDirectory.exists()) {
                if (tempRootPathDirectory.delete()) {
                    logger.debug("({}) The temp root directory is deleted. (path={})", rtspUnitId, tempFileRootPath);
                } else {
                    File[] deleteFolderList = tempRootPathDirectory.listFiles();
                    if (deleteFolderList != null) {
                        for (File file : deleteFolderList) {
                            if (file.delete()) {
                                logger.debug("({}) The temp file is deleted. (path={})", rtspUnitId, file.getAbsolutePath());
                            }
                        }
                    }
                    if (tempRootPathDirectory.delete()) {
                        logger.debug("({}) The temp root directory is deleted. (path={})", rtspUnitId, tempFileRootPath);
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void offer(byte[] data) {
        readBuffer.offer(data);
    }

    public byte[] poll() {
        return readBuffer.poll();
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
