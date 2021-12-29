package com.rtsp.client.gui.component.panel;

import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.gui.buttonlistener.PauseButtonListener;
import com.rtsp.client.gui.buttonlistener.PlayButtonListener;
import com.rtsp.client.gui.buttonlistener.StopButtonListener;
import com.rtsp.client.gui.buttonlistener.VolumeButtonListener;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.*;

public class VideoControlPanel extends JPanel {

    private static final String IMAGE_PATH = System.getProperty("user.dir") + "/src/main/resources/icon/";

    private final ImageIcon playIcon = new ImageIcon(IMAGE_PATH + "playButton.png");
    private final ImageIcon pauseIcon = new ImageIcon(IMAGE_PATH + "pauseButton.png");
    private final ImageIcon stopIcon = new ImageIcon(IMAGE_PATH + "stopButton.png");
    private final ImageIcon volumeIcon = new ImageIcon(IMAGE_PATH + "volumeButton.png");
    private final ImageIcon muteIcon = new ImageIcon(IMAGE_PATH + "muteButton.png");

    private final JButton playButton = new JButton(playIcon);
    private final JButton pauseButton = new JButton(pauseIcon);
    private final JButton stopButton = new JButton(stopIcon);

    private final ProgressBar videoProgressBar = new ProgressBar();
    private final JLabel videoStatus = new JLabel();

    private final JButton volumeButton = new JButton(volumeIcon);
    private final Slider volumeSlider = new Slider();

    private boolean isMute = false;
    private double currentVolume = 100.0;

    public VideoControlPanel() {
        this.setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;

        gridBagConstraints.weighty=0.1;
        gridBagConstraints.gridy=0;

        resizeImageIcon(20, 20);
        initButton(gridBagConstraints);
    }

    private void resizeImageIcon(int width, int height) {
        playIcon.setImage(playIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
        pauseIcon.setImage(pauseIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
        stopIcon.setImage(stopIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
        volumeIcon.setImage(volumeIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
        muteIcon.setImage(muteIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private void initButton(GridBagConstraints gridBagConstraints) {
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        int index = 0;

        gridBagConstraints.weightx=0.1;
        gridBagConstraints.gridx=index++;
        this.add(playButton, gridBagConstraints);

        gridBagConstraints.weightx=0.1;
        gridBagConstraints.gridx=index++;
        this.add(pauseButton, gridBagConstraints);

        gridBagConstraints.weightx=0.1;
        gridBagConstraints.gridx=index++;
        this.add(stopButton, gridBagConstraints);

        index = initProgressBar(gridBagConstraints, index);

        gridBagConstraints.weightx=0.1;
        gridBagConstraints.gridx=index++;
        this.add(volumeButton, gridBagConstraints);

        initSlider(gridBagConstraints, index);


        playButton.addActionListener(new PlayButtonListener());
        pauseButton.addActionListener(new PauseButtonListener());
        stopButton.addActionListener(new StopButtonListener());
        volumeButton.addActionListener(new VolumeButtonListener());
    }

    private int initProgressBar(GridBagConstraints gridBagConstraints, int index) {

        final JFXPanel pFXPanel = new JFXPanel();
        Group root  =  new  Group();
        Scene scene  =  new  Scene(root);
        videoProgressBar.prefWidthProperty().bind(root.getScene().widthProperty());
        videoProgressBar.prefHeightProperty().bind(root.getScene().heightProperty());
        videoProgressBar.setProgress(0.0);

        videoProgressBar.setOnMouseClicked(event -> {
            MediaPlayer videoPlayer = GuiManager.getInstance().getVideoPanel().getMediaPlayer();
            if (videoPlayer != null) {
                videoPlayer.seek(new Duration(videoPlayer.getTotalDuration().toMillis() * (event.getX() / videoProgressBar.getWidth())));
            }
        });

        root.getChildren().add(videoProgressBar);
        pFXPanel.setScene(scene);
        setVideoStatus(0, 0);

        gridBagConstraints.weightx=10.0;
        gridBagConstraints.gridx=index++;
        this.add(pFXPanel, gridBagConstraints);

        gridBagConstraints.weightx=0.1;
        gridBagConstraints.gridx=index++;
        this.add(videoStatus, gridBagConstraints);

        return index;
    }

    private void initSlider(GridBagConstraints gridBagConstraints, int index) {
        final JFXPanel pFXPanel = new JFXPanel();

        Group root  =  new  Group();
        Scene scene  =  new  Scene(root);
        volumeSlider.prefWidthProperty().bind(root.getScene().widthProperty());
        volumeSlider.prefHeightProperty().bind(root.getScene().heightProperty());
        volumeSlider.setValue(currentVolume);

        root.getChildren().add(volumeSlider);
        pFXPanel.setScene(scene);

        gridBagConstraints.weightx=1.0;
        gridBagConstraints.gridx=index;
        this.add(pFXPanel, gridBagConstraints);
    }

    public JButton getPlayButton() {
        return playButton;
    }

    public JButton getPauseButton() {
        return pauseButton;
    }

    public JButton getStopButton() {
        return stopButton;
    }

    public JButton getVolumeButton() {
        return volumeButton;
    }

    public void setVideoProgressBar(double progressValue) {
        Platform.runLater(() -> videoProgressBar.setProgress(progressValue));
    }

    public void setVideoStatus(int curTime, int totalTime) {
        videoStatus.setText(" " + intToTimeString(curTime) + "/" + intToTimeString(totalTime));
    }

    public Slider getVolumeSlider() {
        return volumeSlider;
    }

    public void setVolumeSlider() {

        currentVolume = volumeSlider.getValue();

        volumeSlider.valueProperty().addListener(observable -> {
            MediaPlayer mediaPlayer = GuiManager.getInstance().getVideoPanel().getMediaPlayer();
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(volumeSlider.getValue() / 100);
            }

            if (volumeSlider.getValue() == 0.0 && !isMute()) {
                setMute(true);
            } else if (isMute()) {
                setMute(false);
            }

        });
    }

    public boolean isMute() {
        return isMute;
    }

    public void setMute(boolean mute) {
        isMute = mute;
        volumeButton.setIcon(isMute() ? muteIcon : volumeIcon);
    }

    public double getCurrentVolume() {
        return currentVolume;
    }

    public void setCurrentVolume(double currentVolume) {
        this.currentVolume = currentVolume;
    }

    private String intToTimeString(int time) {
        return String.format("%02d:%02d:%02d",  time / 3600, time / 60, time % 60);
    }
}
