package com.rtsp.client.config;

import com.rtsp.client.service.ServiceManager;
import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private Ini ini = null;

    // Section String
    public static final String SECTION_COMMON = "COMMON"; // COMMON Section 이름
    public static final String SECTION_NETWORK = "NETWORK"; // NETWORK Section 이름
    public static final String SECTION_RTSP = "RTSP"; // NETWORK Section 이름
    public static final String SECTION_REGISTER = "REGISTER"; // REGISTER Section 이름

    // SECTION_COMMON Field String
    private static final String FIELD_SEND_BUF_SIZE = "SEND_BUF_SIZE";
    private static final String FIELD_RECV_BUF_SIZE = "RECV_BUF_SIZE";
    // SECTION_NETWORK Field String
    private static final String FIELD_USER_AGENT = "USER_AGENT";
    private static final String FIELD_LOCAL_LISTEN_IP = "LOCAL_LISTEN_IP";
    private static final String FIELD_LOCAL_LISTEN_PORT = "LOCAL_LISTEN_PORT";
    private static final String FIELD_TARGET_IP = "TARGET_IP";
    private static final String FIELD_TARGET_PORT = "TARGET_PORT";
    // SECTION_COMMON Field String
    private static final String FIELD_STREAM_THREAD_POOL_SIZE = "STREAM_THREAD_POOL_SIZE";
    private static final String FIELD_LOCAL_RTSP_LISTEN_IP = "LOCAL_RTSP_LISTEN_IP";
    private static final String FIELD_LOCAL_RTSP_LISTEN_PORT = "LOCAL_RTSP_LISTEN_PORT";
    private static final String FIELD_TARGET_RTSP_IP = "TARGET_RTSP_IP";
    private static final String FIELD_TARGET_RTSP_PORT = "TARGET_RTSP_PORT";
    private static final String FIELD_URI = "uri";
    // SECTION_COMMON Field String
    private static final String FIELD_MAGIC_COOKIE = "MAGIC_COOKIE";
    private static final String FIELD_HASH_KEY = "HASH_KEY";



    // COMMON
    private int sendBufSize;
    private int recvBufSize;

    // NETWORK
    private String localListenIp;
    private int localListenPort;
    private String targetIp;
    private int targetPort;

    // RTSP
    private String userAgent;
    private int streamThreadPoolSize;
    private String targetRtspIp;
    private int targetRtspPort;
    private String uri;

    // REGISTER
    private String magicCookie;
    private String hashKey;

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
            loadNetworkConfig();
            loadRtspConfig();
            loadRegisterConfig();

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

        logger.debug("Load [{}] config...(OK)", SECTION_COMMON);
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

        uri = getIniValue(SECTION_RTSP, FIELD_URI);

        streamThreadPoolSize = Integer.parseInt(getIniValue(SECTION_RTSP, FIELD_STREAM_THREAD_POOL_SIZE));
        if (streamThreadPoolSize <= 0) {
            streamThreadPoolSize = 10;
        }

        targetRtspIp = getIniValue(SECTION_RTSP, FIELD_TARGET_RTSP_IP);
        targetRtspPort = Integer.parseInt(getIniValue(SECTION_RTSP, FIELD_TARGET_RTSP_PORT));
        if (targetRtspPort <= 0) {
            targetRtspPort = 8500;
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

    public String getUri() {
        return uri;
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
}
