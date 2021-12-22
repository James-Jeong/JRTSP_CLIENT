package com.rtsp.client.gui;

import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.service.scheduler.job.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class VideoPlayer extends Job {

    private static final Logger logger = LoggerFactory.getLogger(VideoPlayer.class);

    public VideoPlayer(String name, int initialDelay, int interval, TimeUnit timeUnit, int priority, int totalRunCount, boolean isLasted) {
        super(name, initialDelay, interval, timeUnit, priority, totalRunCount, isLasted);
    }

    @Override
    public void run() {
        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit == null) {
            return;
        }

        byte[] data = rtspUnit.poll();
        if (data == null || data.length <= 0) {
            return;
        }

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.createImage(data, 0, data.length);
        if (image == null) {
            logger.warn("FAIL TO GET IMAGE");
        } else {
            GuiManager.getInstance().getControlPanel().getIconLabel().setIcon(
                    new ImageIcon(image)
            );
        }
    }
}
