package com.rtsp.client.gui.buttonlistener;

import com.rtsp.client.gui.GuiManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClearButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        GuiManager.getInstance().getMediaPanel().getMediaPlayer().stop();
        GuiManager.getInstance().getMediaPanel().getMediaPlayer().dispose();
        GuiManager.getInstance().getMediaPanel().initMediaView();
        GuiManager.getInstance().getControlPanel().applyClearButtonStatus();
        GuiManager.getInstance().setUploaded(false);
    }
}
