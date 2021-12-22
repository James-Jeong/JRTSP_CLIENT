package com.rtsp.client.gui;

import com.rtsp.client.gui.component.ClientFrame;
import com.rtsp.client.gui.component.panel.ControlPanel;
import com.rtsp.client.gui.component.panel.MediaPanel;

public class GuiManager {

    private static GuiManager guiManager = null;

    private final MediaPanel mediaPanel;
    private final ControlPanel controlPanel;

    private ClientFrame clientFrame;

    private boolean isUploaded = false;

    ////////////////////////////////////////////////////////////////////////////////

    public GuiManager() {
        mediaPanel = new MediaPanel();
        controlPanel = new ControlPanel();
    }

    public static GuiManager getInstance() {
        if (guiManager == null) {
            guiManager = new GuiManager();
        }

        return guiManager;
    }

    public boolean isUploaded() {
        return isUploaded;
    }

    public void setUploaded(boolean uploaded) {
        isUploaded = uploaded;
    }

    // TODO
    public void visibleGui(String title) {
       clientFrame = new ClientFrame(title);
    }

    public MediaPanel getMediaPanel() {
        return mediaPanel;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    public ClientFrame getClientFrame() {
        return clientFrame;
    }

    ////////////////////////////////////////////////////////////////////////////////

}
