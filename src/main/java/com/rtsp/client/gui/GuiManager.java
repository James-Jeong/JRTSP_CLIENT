package com.rtsp.client.gui;

import com.rtsp.client.gui.component.ClientFrame;
import com.rtsp.client.gui.component.panel.ControlPanel;
import com.rtsp.client.gui.component.panel.UriPanel;
import com.rtsp.client.gui.component.panel.VideoPanel;

public class GuiManager {

    private static GuiManager guiManager = null;

    private final VideoPanel videoPanel;
    private final ControlPanel controlPanel;
    private final UriPanel uriPanel;

    private ClientFrame clientFrame;

    private boolean isUploaded = false;

    ////////////////////////////////////////////////////////////////////////////////

    public GuiManager() {
        videoPanel = new VideoPanel();
        controlPanel = new ControlPanel();
        uriPanel = new UriPanel();
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

    public void visibleGui(String title) {
       clientFrame = new ClientFrame(title);
    }

    public VideoPanel getVideoPanel() {
        return videoPanel;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    public UriPanel getUriPanel() {
        return uriPanel;
    }

    public ClientFrame getClientFrame() {
        return clientFrame;
    }

    ////////////////////////////////////////////////////////////////////////////////

}
