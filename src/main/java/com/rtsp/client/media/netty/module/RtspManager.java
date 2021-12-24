package com.rtsp.client.media.netty.module;

import com.fsm.module.StateHandler;
import com.rtsp.client.fsm.RtspEvent;
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
            String rtspUnitId = rtspUnit.getRtspUnitId();
            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
            clearRtspUnit(true);

            rtspUnit = null;
        }
    }

    public void clearRtspUnit(boolean isFinished) {
        if (rtspUnit != null) {
            String rtspUnitId = rtspUnit.getRtspUnitId();
            NettyChannelManager.getInstance().deleteRtpChannel(rtspUnitId);
            NettyChannelManager.getInstance().deleteRtcpChannel(rtspUnitId);

            StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
            if (isFinished) {
                rtspStateHandler.fire(
                        RtspEvent.IDLE,
                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                );
            } else {
                rtspStateHandler.fire(
                        RtspEvent.TEARDOWN_OK,
                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                );
            }

            rtspUnit.clear();
        }
    }

    public RtspUnit getRtspUnit() {
        return rtspUnit;
    }

}
