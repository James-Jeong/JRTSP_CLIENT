package com.rtsp.client.gui.listener;

import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.service.AppInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ConfigButtonListener implements ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(ConfigButtonListener.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        if (GuiManager.getInstance().getTextEditor() != null && !GuiManager.getInstance().getTextEditor().isVisible()) {
            String configFilePath = AppInstance.getInstance().getConfigPath();
            try {
                File configFile = new File(configFilePath);
                if (configFile.exists() && configFile.isFile()) {
                    GuiManager.getInstance().getTextEditor().start();
                }
            } catch (Exception ex) {
                logger.warn("ConfigButtonListener.actionPerformed.Exception", ex);
            }
        }
    }
}
