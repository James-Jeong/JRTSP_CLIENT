package com.rtsp.client;

import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.service.AppInstance;
import com.rtsp.client.service.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtspClientMain {

    private static final Logger logger = LoggerFactory.getLogger(RtspClientMain.class);
    private static final String TITLE = "JRTSP_CLIENT";

    public static void main(String[] args) {
        if (args.length != 2) {
            logger.error("Argument Error. (&0: RtspClientMain, &1: config_path)");
            return;
        }

        String configPath = args[1].trim();
        logger.debug("| Config path: {}", configPath);
        ConfigManager configManager = new ConfigManager(configPath);

        AppInstance appInstance = AppInstance.getInstance();
        appInstance.setConfigManager(configManager);


        GuiManager guiManager = GuiManager.getInstance();
        guiManager.visibleGui(TITLE);


        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.loop();

    }

}
