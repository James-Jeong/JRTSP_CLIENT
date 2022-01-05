package com.rtsp.client.service;

import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.service.scheduler.job.Job;
import com.rtsp.client.system.SystemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author jamesj
 * @class public class ServiceHaHandler extends TaskUnit
 * @brief ServiceHaHandler
 */
public class HaHandler extends Job {

    private static final Logger logger = LoggerFactory.getLogger(HaHandler.class);

    private final NettyChannelManager nettyChannelManager = NettyChannelManager.getInstance();

    ////////////////////////////////////////////////////////////////////////////////

    public HaHandler(String name, int initialDelay, int interval, TimeUnit timeUnit, int priority, int totalRunCount, boolean isLasted) {
        super(name, initialDelay, interval, timeUnit, priority, totalRunCount, isLasted);
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void run () {
        SystemManager systemManager = SystemManager.getInstance();

        String cpuUsageStr = systemManager.getCpuUsage();
        String memoryUsageStr = systemManager.getHeapMemoryUsage();

        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        String curState = null;
        if (rtspUnit != null) {
            curState = rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId()).getCurState();
        }
        logger.debug("| cpu=[{}], mem=[{}], thread=[{}], RtspState=[{}]",
                cpuUsageStr, memoryUsageStr, Thread.activeCount(), curState);
    }

}
