package com.rtsp.client.gui.component.panel;

import com.rtsp.client.gui.base.ButtonType;
import com.rtsp.client.gui.buttonlistener.*;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {

    private final JButton registerButton = new JButton(ButtonType.REGISTER);
    private final JButton playButton = new JButton(ButtonType.PLAY);
    private final JButton pauseButton = new JButton(ButtonType.PAUSE);
    private final JButton stopButton = new JButton(ButtonType.STOP);
    private final JButton unregisterButton = new JButton(ButtonType.UNREGISTER);
    private final JButton finishButton = new JButton(ButtonType.FINISH);

    public ControlPanel() {
        GridLayout gridLayout = new GridLayout(6, 1);
        gridLayout.setVgap(10);
        gridLayout.setHgap(5);
        setLayout(gridLayout);
        initButton();
    }

    public void initButton() {
        registerButton.addActionListener(new RegisterButtonListener());
        playButton.addActionListener(new PlayButtonListener());
        pauseButton.addActionListener(new PauseButtonListener());
        stopButton.addActionListener(new StopButtonListener());
        unregisterButton.addActionListener(new UnregisterButtonListener());
        finishButton.addActionListener(new FinishButtonListener());

        initButtonStatus();

        this.add(registerButton);
        this.add(playButton);
        this.add(pauseButton);
        this.add(stopButton);
        this.add(unregisterButton);
        this.add(finishButton);
    }

    public void initButtonStatus() {
        registerButton.setEnabled(true);
        playButton.setEnabled(false);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
        unregisterButton.setEnabled(false);
        finishButton.setEnabled(true);
    }

    public void applyRegistrationButtonStatus() {
        registerButton.setEnabled(false);
        playButton.setEnabled(true);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
        finishButton.setEnabled(true);

        // TODO 등록 해제 기능은 아직 미지원
        //unregisterButton.setEnabled(true);
        unregisterButton.setEnabled(false);
    }

    public void applyPlayButtonStatus() {
        registerButton.setEnabled(false);
        playButton.setEnabled(false);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);
        finishButton.setEnabled(false);

        // TODO 등록 해제 기능은 아직 미지원
        //unregisterButton.setEnabled(true);
        unregisterButton.setEnabled(false);
    }

    public void applyPauseButtonStatus() {
        registerButton.setEnabled(false);
        playButton.setEnabled(true);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(true);
        finishButton.setEnabled(false);

        // TODO 등록 해제 기능은 아직 미지원
        //unregisterButton.setEnabled(true);
        unregisterButton.setEnabled(false);
    }
}
