package com.rtsp.client.media.netty.module.base;

import com.fsm.StateManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.file.RtspFileManager;
import com.rtsp.client.fsm.RtspFsmManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspNettyChannel;
import com.rtsp.client.media.sdp.SdpParser;
import com.rtsp.client.media.sdp.base.Sdp;
import com.rtsp.client.service.AppInstance;
import com.rtsp.client.service.base.ConcurrentCyclicFIFO;
import io.lindstrom.m3u8.model.MediaSegment;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * @class public class RtspUnit
 * @brief RtspUnit class
 */
public class RtspUnit {

    private static final Logger logger = LoggerFactory.getLogger(RtspUnit.class);

    public static final int MAX_SESSION_ID = 100000;
    public static final String VIDEO_JOB_KEY = "VIDEO_PLAY";

    private String uri = null;
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
    private boolean isRegistered = false;

    private final ConcurrentCyclicFIFO<byte[]> m3u8ReadBuffer = new ConcurrentCyclicFIFO<>();
    private final ConcurrentCyclicFIFO<byte[]> tsReadBuffer = new ConcurrentCyclicFIFO<>();

    private final RtspFileManager fileManager;
    private String fileNameOnly;
    private String tempFileRootPath;
    private String m3u8FilePath;
    private String tsFilePath;
    private String mp4FilePath;
    private int tsFileLimit = 0;
    private List<MediaSegment> mediaSegmentList = null;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspUnit(String targetIp, int targetPort) {
        this.rtspUnitId = UUID.randomUUID().toString();
        this.rtspStateUnitId = UUID.randomUUID().toString();
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.fileManager = new RtspFileManager(rtspUnitId);
        rtspFsmManager.init(this);

        logger.debug("({}) RtspUnit is created. (stateUnitId={}, targetIp={}, targetPort={})", rtspUnitId, rtspStateUnitId, targetIp, targetPort);
    }

    public void open() {
        rtspChannel = NettyChannelManager.getInstance().openRtspChannel(
                rtspUnitId,
                targetIp,
                targetPort
        );
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;

        String curUri = uri;
        if (uri.contains("*")) {
            curUri = uri.replaceAll("[*]", " ");
        }

        fileNameOnly = fileManager.getFileNameExceptForExtension(curUri);

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        tempFileRootPath = configManager.getTempRootPath() + fileNameOnly + "_tmp";
        File tempRootPathDirectory = new File(tempFileRootPath);
        if (!tempRootPathDirectory.exists()) {
            if (tempRootPathDirectory.mkdirs()) {
                logger.debug("({}) New temp root directory is created. ({})", rtspUnitId, tempFileRootPath);
            }
        }

        m3u8FilePath = tempFileRootPath + fileNameOnly + ".m3u8";
        tsFilePath = tempFileRootPath + fileNameOnly + "%d.ts";
        mp4FilePath = tempFileRootPath + fileNameOnly + ".mp4";;
    }

    public List<MediaSegment> getMediaSegmentList() {
        return mediaSegmentList;
    }

    public void setMediaSegmentList(List<MediaSegment> mediaSegmentList) {
        this.mediaSegmentList = mediaSegmentList;
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

    public String getMp4FilePath() {
        return mp4FilePath;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
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

    public RtspFileManager getFileManager() {
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
            isStarted = false;
            isPaused = false;
            mediaSegmentList = null;
            rtpTimeoutStopWatch.reset();
            m3u8ReadBuffer.clear();
            tsReadBuffer.clear();
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

        File tempRootPathDirectory = new File(tempFileRootPath);
        if (configManager.isDeleteM3u8() && configManager.isDeleteTs() && configManager.isDeleteMp4()) {
            if (tempRootPathDirectory.exists()) {
                if (tempRootPathDirectory.delete()) {
                    logger.debug("({}) The temp root directory is deleted. (path={})", rtspUnitId, tempFileRootPath);
                }
            }
        }

        File[] deleteFolderList = tempRootPathDirectory.listFiles();
        if (deleteFolderList != null) {
            for (File file : deleteFolderList) {
                if (file.delete()) {
                    logger.debug("({}) The temp file is deleted. (path={})", rtspUnitId, file.getAbsolutePath());
                }
            }
        }

        if (deleteFolderList == null || deleteFolderList.length == 0) {
            if (tempRootPathDirectory.delete()) {
                logger.debug("({}) The temp root directory is deleted. (path={})", rtspUnitId, tempFileRootPath);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void offerToM3U8Buffer(byte[] data) {
        m3u8ReadBuffer.offer(data);
    }

    public byte[] pollFromM3U8Buffer() {
        return m3u8ReadBuffer.poll();
    }

    public void offerToTsBuffer(byte[] data) {
        tsReadBuffer.offer(data);
    }

    public byte[] pollFromTsBuffer() {
        return tsReadBuffer.poll();
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
