package com.rtsp.client.gui.buttonlistener;

import com.rtsp.client.service.ServiceManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FinishButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        ServiceManager.getInstance().stop();
        System.exit(1);
    }
}
