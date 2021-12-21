package com.rtsp.client.gui.component.panel;

import com.rtsp.client.gui.GuiManager;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MediaPanel extends JPanel {

    private final JFXPanel vFXPanel = new JFXPanel();

    private Media media;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;

    public MediaPanel() {
        this.setLayout(new BorderLayout());
        this.add(vFXPanel, BorderLayout.CENTER);

        initMediaView();
    }

    private void initMediaView() {
        mediaView = new MediaView();

        // resize video based on screen size
        final DoubleProperty width = mediaView.fitWidthProperty();
        final DoubleProperty height = mediaView.fitHeightProperty();

        width.bind(Bindings.selectDouble(mediaView.sceneProperty(), "width"));
        height.bind(Bindings.selectDouble(mediaView.sceneProperty(), "height"));
        mediaView.setPreserveRatio(true);


        // add video to stackPane
        StackPane root = new StackPane();
        root.getChildren().add(mediaView);
        final Scene scene = new Scene(root);

        vFXPanel.setScene(scene);
    }

    public void initMediaPlayer(String path) {
        File videoFile = new File(path);
        media = new Media(videoFile.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnStopped(() -> {
            GuiManager.getInstance().getControlPanel().applyRegistrationButtonStatus();
        });
        mediaView.setMediaPlayer(mediaPlayer);
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}
