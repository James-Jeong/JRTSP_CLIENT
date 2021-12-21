package com.rtsp.client.media.netty.module;

import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.base.RtspUnit;

/**
 * @class public class RtspManager
 * @brief RtspManager class
 */
public class RtspManager {

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

    public void openRtspUnit(String ip, int port) {
        if (rtspUnit == null) {
            rtspUnit = new RtspUnit(ip, port);
            rtspUnit.getStateManager().addStateUnit(
                    rtspUnit.getRtspStateUnitId(),
                    rtspUnit.getStateManager().getStateHandler(RtspState.NAME).getName(),
                    RtspState.IDLE,
                    null
            );
        }
    }

    public void closeRtspUnit() {
        if (rtspUnit != null) {
            NettyChannelManager.getInstance().deleteRtspChannel(
                    rtspUnit.getRtspChannel().getListenIp()
                            + "_" +
                            rtspUnit.getRtspChannel().getListenPort()
            );
        }
    }

    public RtspUnit getRtspUnit() {
        return rtspUnit;
    }

}
