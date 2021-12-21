package com.rtsp.client.gui.buttonlistener;

import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.gui.component.panel.MediaPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UnregisterButtonListener implements ActionListener {

    private int number = 1;

    @Override
    public void actionPerformed(ActionEvent e) {
        //MediaPanel mediaPanel = GuiManager.getInstance().getMediaPanel();
        /*if ( number % 2 == 0) {
            mediaPanel.initMediaPlayer("rtsp_client/src/main/resources/video/Seoul.mp4");
        } else {
            mediaPanel.initMediaPlayer("rtsp_client/src/main/resources/video/rabbit.mp4");
        }
        number++;*/

        GuiManager.getInstance().getControlPanel().initButtonStatus();
    }
}
