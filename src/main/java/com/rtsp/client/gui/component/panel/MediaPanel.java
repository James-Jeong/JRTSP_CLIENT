package com.rtsp.client.gui.component.panel;

import com.rtsp.client.gui.GuiManager;

import javax.swing.*;
import java.awt.*;

public class MediaPanel extends JPanel {

    public MediaPanel() {
        // layout 설정
        BorderLayout borderLayout = new BorderLayout();
        setLayout(borderLayout);
        // panel 설정
        GuiManager guiManager = GuiManager.getInstance();
        add(guiManager.getVideoPanel(), BorderLayout.CENTER);
        add(guiManager.getControlPanel(), BorderLayout.EAST);
    }
}