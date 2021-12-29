package com.rtsp.client.media.netty.module;

import com.fsm.module.StateHandler;
import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.fsm.RtspEvent;
import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.service.AppInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @class public class RtspManager
 * @brief RtspManager class
 */
public class RtspManager {

    private static final Logger log = LoggerFactory.getLogger(RtspManager.class);

    private static RtspManager rtspManager = null;

    private RtspUnit rtspUnit = null;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspManager() {
        // Nothing
    }

    public static RtspManager getInstance ( ) {
        if (rtspManager == null) {
            rtspManager = new RtspManager();
        }

        return rtspManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public RtspUnit openRtspUnit(String ip, int port) {
        if (rtspUnit == null) {
            rtspUnit = new RtspUnit(ip, port);
            rtspUnit.getStateManager().addStateUnit(
                    rtspUnit.getRtspStateUnitId(),
                    rtspUnit.getStateManager().getStateHandler(RtspState.NAME).getName(),
                    RtspState.IDLE,
                    null
            );
        }

        return rtspUnit;
    }

    public void closeRtspUnit() {
        if (rtspUnit != null) {
            rtspUnit.setRegistered(false);
            String rtspUnitId = rtspUnit.getRtspUnitId();
            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
            clearRtspUnit(true, true);

            rtspUnit = null;
        }
    }

    public void clearRtspUnit(boolean isStopped, boolean isFinished) {
        if (rtspUnit != null) {
            if (isStopped) {
                String rtspUnitId = rtspUnit.getRtspUnitId();
                NettyChannelManager.getInstance().deleteRtpChannel(rtspUnitId);
                NettyChannelManager.getInstance().deleteRtcpChannel(rtspUnitId);

                StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
                if (isFinished) {
                    rtspStateHandler.fire(
                            RtspEvent.IDLE,
                            rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                    );
                    rtspUnit.setRegistered(false);
                } else {
                    rtspStateHandler.fire(
                            RtspEvent.TEARDOWN_OK,
                            rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                    );
                }
            }

            rtspUnit.clear(isStopped);
        }
    }

    public RtspUnit getRtspUnit() {
        return rtspUnit;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public static String convertLocalPathToRtspPath(String localPath) {
        if (localPath == null || localPath.isEmpty()) {
            return null;
        }

        if (localPath.startsWith("rtsp://")) {
            return localPath;
        }

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        String result = "rtsp://" + configManager.getTargetRtspIp() + ":" + configManager.getTargetRtspPort();
        return result + localPath;
    }

}
