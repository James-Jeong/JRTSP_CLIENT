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
        /*if (args.length != 2) {
            logger.error("Argument Error. (&0: RtspClientMain, &1: config_path)");
            return;
        }*/

        String curUserDir = System.getProperty("user.dir");
        logger.debug("curUserDir: {}", curUserDir);

        AppInstance appInstance = AppInstance.getInstance();
        String configPath = "./config/user_conf.ini";
        if (args != null && args.length > 0) {
            configPath = args[1].trim();
            logger.debug("| Config path: {}", configPath);
            appInstance.setApplicationMode(false);
        }
        ConfigManager configManager = new ConfigManager(configPath);

        appInstance.setConfigManager(configManager);

        GuiManager guiManager = GuiManager.getInstance();
        guiManager.visibleGui(TITLE);

        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.loop();
    }

}
