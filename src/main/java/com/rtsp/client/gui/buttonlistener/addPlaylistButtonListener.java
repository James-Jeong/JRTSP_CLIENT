package com.rtsp.client.gui.buttonlistener;

import com.rtsp.client.gui.GuiManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class addPlaylistButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        GuiManager guiManager = GuiManager.getInstance();
        guiManager.getPlaylistPanel().addPlaylist(0, guiManager.getUriPanel().getUriTextField().getText());
    }
}
