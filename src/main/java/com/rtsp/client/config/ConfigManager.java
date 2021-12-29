package com.rtsp.client.config;

import com.rtsp.client.media.sdp.SdpParser;
import com.rtsp.client.media.sdp.base.Sdp;
import com.rtsp.client.service.ServiceManager;
import org.apache.commons.net.ntp.TimeStamp;
import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    public static final int MP2T_TYPE = 33; //RTP payload type for MJPEG video
    public static final String MP2T_TAG = "MP2T"; //RTP payload tag for MJPEG video

    private Ini ini = null;

    // Section String
    public static final String SECTION_COMMON = "COMMON"; // COMMON Section 이름
    public static final String SECTION_FFMPEG = "FFMPEG"; // FFMPEG Section 이름
    public static final String SECTION_NETWORK = "NETWORK"; // NETWORK Section 이름
    public static final String SECTION_RTSP = "RTSP"; // NETWORK Section 이름
    public static final String SECTION_REGISTER = "REGISTER"; // REGISTER Section 이름
    private static final String SECTION_SDP = "SDP"; // SDP Section 이름

    // SECTION_COMMON Field String
    private static final String FIELD_SEND_BUF_SIZE = "SEND_BUF_SIZE";
    private static final String FIELD_RECV_BUF_SIZE = "RECV_BUF_SIZE";
    private static final String FIELD_PLAYLIST_SIZE = "PLAYLIST_SIZE";

    // SECTION_FFMPEG Field String
    public static final String FIELD_FFMPEG_PATH = "FFMPEG_PATH";
    public static final String FIELD_FFPROBE_PATH = "FFPROBE_PATH";
    public static final String FIELD_TEMP_ROOT_PATH = "TEMP_ROOT_PATH";
    public static final String FIELD_DELETE_M3U8 = "DELETE_M3U8";
    public static final String FIELD_DELETE_TS = "DELETE_TS";
    public static final String FIELD_DELETE_MP4 = "DELETE_MP4";

    // SECTION_NETWORK Field String
    private static final String FIELD_LOCAL_LISTEN_IP = "LOCAL_LISTEN_IP";
    private static final String FIELD_LOCAL_LISTEN_PORT = "LOCAL_LISTEN_PORT";
    private static final String FIELD_TARGET_IP = "TARGET_IP";
    private static final String FIELD_TARGET_PORT = "TARGET_PORT";

    // SECTION_RTSP Field String
    private static final String FIELD_USER_AGENT = "USER_AGENT";
    private static final String FIELD_STREAM_THREAD_POOL_SIZE = "STREAM_THREAD_POOL_SIZE";
    private static final String FIELD_LOCAL_RTSP_PORT = "LOCAL_RTSP_PORT";
    private static final String FIELD_TARGET_RTSP_IP = "TARGET_RTSP_IP";
    private static final String FIELD_TARGET_RTSP_PORT = "TARGET_RTSP_PORT";
    private static final String FIELD_URI_LIMIT = "URI_LIMIT";
    private static final String FIELD_RTP_TIMEOUT = "RTP_TIMEOUT";

    // SECTION_COMMON Field String
    private static final String FIELD_MAGIC_COOKIE = "MAGIC_COOKIE";
    private static final String FIELD_HASH_KEY = "HASH_KEY";

    // COMMON
    private int sendBufSize;
    private int recvBufSize;
    private int playlistSize;

    // FFMPEG
    private String ffmpegPath = null;
    private String ffprobePath = null;
    private String tempRootPath = null;
    private boolean deleteM3u8 = false;
    private boolean deleteTs = false;
    private boolean deleteMp4 = false;

    // NETWORK
    private String localListenIp;
    private int localListenPort;
    private String targetIp;
    private int targetPort;

    // RTSP
    private String userAgent;
    private int streamThreadPoolSize;
    private int localRtspPort;
    private String targetRtspIp;
    private int targetRtspPort;
    private int uriLimit;
    private long rtpTimeout; // sec

    // REGISTER
    private String magicCookie;
    private String hashKey;

    // SDP
    private final SdpParser sdpParser = new SdpParser();
    private String version;
    private String origin;
    private String session;
    private String time;
    private String connection;
    private String media;
    String[] mp2tAttributeList;
    String[] attributeList;

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public AuditConfig(String configPath)
     * @brief AuditConfig 생성자 함수
     * @param configPath Config 파일 경로 이름
     */
    public ConfigManager(String configPath) {
        File iniFile = new File(configPath);
        if (!iniFile.isFile() || !iniFile.exists()) {
            logger.warn("Not found the config path. (path={})", configPath);
            return;
        }

        try {
            this.ini = new Ini(iniFile);

            loadCommonConfig();
            loadFfmpegConfig();
            loadNetworkConfig();
            loadRtspConfig();
            loadRegisterConfig();
            loadSdpConfig();

            logger.info("Load config [{}]", configPath);
        } catch (IOException e) {
            logger.error("ConfigManager.IOException", e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn private void loadCommonConfig()
     * @brief COMMON Section 을 로드하는 함수
     */
    private void loadCommonConfig() {
        sendBufSize = Integer.parseInt(getIniValue(SECTION_COMMON, FIELD_SEND_BUF_SIZE));
        if (sendBufSize <= 0) {
            sendBufSize = 33554432;
        }

        recvBufSize = Integer.parseInt(getIniValue(SECTION_COMMON, FIELD_RECV_BUF_SIZE));
        if (recvBufSize <= 0) {
            recvBufSize = 16777216;
        }

        playlistSize = Integer.parseInt(getIniValue(SECTION_COMMON, FIELD_PLAYLIST_SIZE));
        if (playlistSize <= 0) {
            playlistSize = 10;
        }

        logger.debug("Load [{}] config...(OK)", SECTION_COMMON);
    }

    /**
     * @fn private void loadFfmpegConfig()
     * @brief FFMPEG Section 을 로드하는 함수
     */
    private void loadFfmpegConfig() {
        this.ffmpegPath = getIniValue(SECTION_FFMPEG, FIELD_FFMPEG_PATH);
        if (this.ffmpegPath == null) {
            return;
        }

        this.ffprobePath = getIniValue(SECTION_FFMPEG, FIELD_FFPROBE_PATH);
        if (this.ffprobePath == null) {
            return;
        }

        this.tempRootPath = getIniValue(SECTION_FFMPEG, FIELD_TEMP_ROOT_PATH);
        if (this.tempRootPath == null) {
            return;
        }

        this.deleteM3u8 = Boolean.parseBoolean(getIniValue(SECTION_FFMPEG, FIELD_DELETE_M3U8));
        this.deleteTs = Boolean.parseBoolean(getIniValue(SECTION_FFMPEG, FIELD_DELETE_TS));
        this.deleteMp4 = Boolean.parseBoolean(getIniValue(SECTION_FFMPEG, FIELD_DELETE_MP4));

        logger.debug("Load [{}] config...(OK)", SECTION_FFMPEG);
    }

    /**
     * @fn private void loadCommonConfig()
     * @brief NETWORK Section 을 로드하는 함수
     */
    private void loadNetworkConfig() {
        localListenIp = getIniValue(SECTION_NETWORK, FIELD_LOCAL_LISTEN_IP);
        localListenPort = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_LOCAL_LISTEN_PORT));
        if (localListenPort <= 0) {
            localListenPort = 9000;
        }

        targetIp = getIniValue(SECTION_NETWORK, FIELD_TARGET_IP);
        targetPort = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_TARGET_PORT));
        if (targetPort <= 0) {
            targetPort = 9100;
        }

        logger.debug("Load [{}] config...(OK)", SECTION_NETWORK);
    }

    /**
     * @fn private void loadRtspConfig()
     * @brief RTSP Section 을 로드하는 함수
     */
    private void loadRtspConfig() {
        userAgent = getIniValue(SECTION_RTSP, FIELD_USER_AGENT);

        streamThreadPoolSize = Integer.parseInt(getIniValue(SECTION_RTSP, FIELD_STREAM_THREAD_POOL_SIZE));
        if (streamThreadPoolSize <= 0) {
            streamThreadPoolSize = 10;
        }

        localRtspPort = Integer.parseInt(getIniValue(SECTION_RTSP, FIELD_LOCAL_RTSP_PORT));
        if (localRtspPort <= 0) {
            localRtspPort = 8554;
        }

        targetRtspIp = getIniValue(SECTION_RTSP, FIELD_TARGET_RTSP_IP);
        targetRtspPort = Integer.parseInt(getIniValue(SECTION_RTSP, FIELD_TARGET_RTSP_PORT));
        if (targetRtspPort <= 0) {
            targetRtspPort = 8554;
        }

        uriLimit = Integer.parseInt(getIniValue(SECTION_RTSP, FIELD_URI_LIMIT));
        if (uriLimit <= 0) {
            uriLimit = 100;
        }

        rtpTimeout = Integer.parseInt(getIniValue(SECTION_RTSP, FIELD_RTP_TIMEOUT));
        if (rtpTimeout < 0) {
            rtpTimeout = 2000;
        }

        logger.debug("Load [{}] config...(OK)", SECTION_RTSP);
    }

    /**
     * @fn private void loadRegisterConfig()
     * @brief COMMON Section 을 로드하는 함수
     */
    private void loadRegisterConfig() {
        magicCookie = getIniValue(SECTION_REGISTER, FIELD_MAGIC_COOKIE);
        hashKey = getIniValue(SECTION_REGISTER, FIELD_HASH_KEY);

        logger.debug("Load [{}] config...(OK)", SECTION_REGISTER);
    }

    private void loadSdpConfig() {
        version = getIniValue(SECTION_SDP, "VERSION");
        if (version == null) {
            logger.error("[SECTION_SDP] VERSION IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }
        version = "v=" + version + "\r\n";

        origin = getIniValue(SECTION_SDP, "ORIGIN");
        if (origin == null) {
            logger.error("[SECTION_SDP] ORIGIN IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        session = getIniValue(SECTION_SDP, "SESSION");
        if (session == null) {
            logger.error("[SECTION_SDP] SESSION IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }
        session = "s=" + session + "\r\n";

        time = getIniValue(SECTION_SDP, "TIME");
        if (time == null) {
            logger.error("[SECTION_SDP] TIME IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }
        time = "t=" + time + "\r\n";

        connection = getIniValue(SECTION_SDP, "CONNECTION");
        if (connection == null) {
            logger.error("[SECTION_SDP] CONNECTION IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        media = getIniValue(SECTION_SDP, "MEDIA");
        if (media == null) {
            logger.error("[SECTION_SDP] MEDIA IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        mp2tAttributeList = new String[1];
        String attributeMp2t = getIniValue(SECTION_SDP, String.format("ATTR_MP2T_%d", 0));
        if (attributeMp2t == null) {
            logger.error("[SECTION_SDP] ATTR_MP2T_{} IS NOT DEFINED IN THE LOCAL SDP.", 0);
            System.exit(1);
        }
        mp2tAttributeList[0] = attributeMp2t;

        int attrCount = Integer.parseInt(getIniValue(SECTION_SDP, "ATTR_COUNT"));
        if (attrCount < 0) {
            logger.error("[SECTION_SDP] ATTR_COUNT IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        attributeList = new String[attrCount];
        for (int i = 0; i < attrCount; i++) {
            String attribute = getIniValue(SECTION_SDP, String.format("ATTR_%d", i));
            if (attribute != null) {
                attributeList[i] = attribute;
            }
        }
    }

    public Sdp loadLocalSdpConfig(String id, int localPort) {
        try {
            StringBuilder sdpStr = new StringBuilder();

            // 1) Session
            // 1-1) Version
            sdpStr.append(version);

            // 1-2) Origin
            /*
                - Using NTP Timestamp
                [RFC 4566]
                  <sess-id> is a numeric string such that the tuple of <username>,
                  <sess-id>, <nettype>, <addrtype>, and <unicast-address> forms a
                  globally unique identifier for the session.  The method of
                  <sess-id> allocation is up to the creating tool, but it has been
                  suggested that a Network Time Protocol (NTP) format timestamp be
                  used to ensure uniqueness.
             */
            String originSessionId = String.valueOf(TimeStamp.getCurrentTime().getTime());
            String curOrigin = String.format(this.origin, originSessionId, localListenIp);
            curOrigin = "o=" + curOrigin + "\r\n";
            sdpStr.append(curOrigin);

            // 1-3) Session
            sdpStr.append(session);

            // 3) Media
            // 3-1) Connection
            String connection = String.format(this.connection, localListenIp);
            connection = "c=" + connection + "\r\n";
            sdpStr.append(connection);

            // 2) Time
            // 2-1) Time
            sdpStr.append(time);

            // 3) Media
            // 3-2) Media
            sdpStr.append("m=");
            String media = String.format(this.media, localPort, MP2T_TYPE);
            sdpStr.append(media);
            sdpStr.append("\r\n");

            // 3-3) Attribute
            sdpStr.append("a=");
            sdpStr.append(String.format(mp2tAttributeList[0], MP2T_TYPE));
            sdpStr.append("\r\n");

            for (String attribute : attributeList) {
                sdpStr.append("a=");
                sdpStr.append(attribute);
                sdpStr.append("\r\n");
            }

            Sdp localSdp = null;
            try {
                localSdp = sdpParser.parseSdp(id, null, null, sdpStr.toString());
                logger.debug("({}) Local SDP=\n{}", id, localSdp.getData(false));
            } catch (Exception e) {
                logger.error("({}) Fail to parse the local sdp. ({})", id, sdpStr, e);
                System.exit(1);
            }
            return localSdp;
        } catch (Exception e) {
            logger.warn("Fail to load the local sdp.", e);
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn private String getIniValue(String section, String key)
     * @brief INI 파일에서 지정한 section 과 key 에 해당하는 value 를 가져오는 함수
     * @param section Section
     * @param key Key
     * @return 성공 시 value, 실패 시 null 반환
     */
    private String getIniValue(String section, String key) {
        String value = ini.get(section,key);
        if (value == null) {
            logger.warn("[ {} ] \" {} \" is null.", section, key);
            ServiceManager.getInstance().stop();
            System.exit(1);
            return null;
        }

        value = value.trim();
        logger.debug("\tGet Config [{}] > [{}] : [{}]", section, key, value);
        return value;
    }

    /**
     * @fn public void setIniValue(String section, String key, String value)
     * @brief INI 파일에 새로운 value 를 저장하는 함수
     * @param section Section
     * @param key Key
     * @param value Value
     */
    public void setIniValue(String section, String key, String value) {
        try {
            ini.put(section, key, value);
            ini.store();

            logger.debug("\tSet Config [{}] > [{}] : [{}]", section, key, value);
        } catch (IOException e) {
            logger.warn("Fail to set the config. (section={}, field={}, value={})", section, key, value);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public int getLocalRtspPort() {
        return localRtspPort;
    }

    public boolean isDeleteMp4() {
        return deleteMp4;
    }

    public long getRtpTimeout() {
        return rtpTimeout;
    }

    public void setRtpTimeout(long rtpTimeout) {
        this.rtpTimeout = rtpTimeout;
    }

    public boolean isDeleteM3u8() {
        return deleteM3u8;
    }

    public boolean isDeleteTs() {
        return deleteTs;
    }

    public String getTempRootPath() {
        return tempRootPath;
    }

    public void setTempRootPath(String tempRootPath) {
        this.tempRootPath = tempRootPath;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public String getFfprobePath() {
        return ffprobePath;
    }

    public void setFfprobePath(String ffprobePath) {
        this.ffprobePath = ffprobePath;
    }

    public void setIni(Ini ini) {
        this.ini = ini;
    }

    public int getSendBufSize() {
        return sendBufSize;
    }

    public void setSendBufSize(int sendBufSize) {
        this.sendBufSize = sendBufSize;
    }

    public int getRecvBufSize() {
        return recvBufSize;
    }

    public void setRecvBufSize(int recvBufSize) {
        this.recvBufSize = recvBufSize;
    }

    public int getPlaylistSize() {
        return playlistSize;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getStreamThreadPoolSize() {
        return streamThreadPoolSize;
    }

    public void setStreamThreadPoolSize(int streamThreadPoolSize) {
        this.streamThreadPoolSize = streamThreadPoolSize;
    }

    public String getTargetRtspIp() {
        return targetRtspIp;
    }

    public void setTargetRtspIp(String targetRtspIp) {
        this.targetRtspIp = targetRtspIp;
    }

    public int getTargetRtspPort() {
        return targetRtspPort;
    }

    public void setTargetRtspPort(int targetRtspPort) {
        this.targetRtspPort = targetRtspPort;
    }

    public String getLocalListenIp() {
        return localListenIp;
    }

    public void setLocalListenIp(String localListenIp) {
        this.localListenIp = localListenIp;
    }

    public int getLocalListenPort() {
        return localListenPort;
    }

    public void setLocalListenPort(int localListenPort) {
        this.localListenPort = localListenPort;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public String getMagicCookie() {
        return magicCookie;
    }

    public String getHashKey() {
        return hashKey;
    }

    public int getUriLimit() {
        return uriLimit;
    }
}
