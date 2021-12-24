package com.rtsp.client.gui.component;

import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.gui.component.panel.MediaPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class ClientFrame extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(ClientFrame.class);

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 600;

    public ClientFrame(String title) {
        super(title);

        // 프레임 크기
        setSize(WIDTH, HEIGHT);
        // 화면 가운데 배치
        setLocationRelativeTo(null);
        // 닫을 때 메모리에서 제거되도록 설정
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        // layout 설정
        BorderLayout borderLayout = new BorderLayout();
        setLayout(borderLayout);
        // panel 설정
        GuiManager guiManager = GuiManager.getInstance();

        add(new MediaPanel(), BorderLayout.CENTER);
        add(guiManager.getUriPanel(), BorderLayout.SOUTH);

        // 보이게 설정
        setVisible(true);
    }
}
