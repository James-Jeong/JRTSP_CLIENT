package com.rtsp.client.gui;

import com.rtsp.client.gui.component.ClientFrame;
import com.rtsp.client.gui.component.panel.*;

import java.awt.*;

public class GuiManager {

    private static GuiManager guiManager = null;

    private final VideoPanel videoPanel;
    private final VideoControlPanel videoControlPanel;
    private final ControlPanel controlPanel;
    private final PlaylistPanel playlistPanel;
    private final UriPanel uriPanel;

    private ClientFrame clientFrame;

    private boolean isUploaded = false;

    ////////////////////////////////////////////////////////////////////////////////

    public GuiManager() {
        videoPanel = new VideoPanel();
        videoControlPanel = new VideoControlPanel();
        videoPanel.add(videoControlPanel, BorderLayout.SOUTH);
        controlPanel = new ControlPanel();
        playlistPanel = new PlaylistPanel();
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

    public VideoControlPanel getVideoControlPanel() {
        return videoControlPanel;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    public PlaylistPanel getPlaylistPanel() {
        return playlistPanel;
    }

    public UriPanel getUriPanel() {
        return uriPanel;
    }

    public ClientFrame getClientFrame() {
        return clientFrame;
    }

    public String getSelectPlaylist() {
        return playlistPanel.getSelectedUri();
    }

    ////////////////////////////////////////////////////////////////////////////////

}
