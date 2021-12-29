package com.rtsp.client.gui.component.panel;

import com.rtsp.client.gui.GuiManager;

import javax.swing.*;
import java.awt.*;

public class MediaPanel extends JPanel {

    public MediaPanel() {
        BorderLayout borderLayout = new BorderLayout();
        setLayout(borderLayout);

        GuiManager guiManager = GuiManager.getInstance();
        add(guiManager.getVideoPanel(), BorderLayout.CENTER);
        add(new MediaControlPanel(), BorderLayout.EAST);
    }
}