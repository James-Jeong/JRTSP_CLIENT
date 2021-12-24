package com.rtsp.client.gui.component.panel;

import com.fsm.module.StateHandler;
import com.rtsp.client.fsm.RtspEvent;
import com.rtsp.client.fsm.RtspState;
import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.RtspNettyChannel;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class VideoPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(MediaPanel.class);

    private final JFXPanel vFXPanel = new JFXPanel();

    private Media media;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;

    public VideoPanel() {
        this.setLayout(new BorderLayout());
        this.add(vFXPanel, BorderLayout.CENTER);
        vFXPanel.setVisible(true);

        initMediaView();
    }

    public void initMediaView() {
        mediaView = new MediaView();

        // add video to stackPane
        StackPane root = new StackPane();
        root.getChildren().add(mediaView);
        final Scene scene = new Scene(root);

        // resize video based on screen size
        final DoubleProperty width = mediaView.fitWidthProperty();
        final DoubleProperty height = mediaView.fitHeightProperty();

        width.bind(Bindings.selectDouble(mediaView.sceneProperty(), "width"));
        height.bind(Bindings.selectDouble(mediaView.sceneProperty(), "height"));
        mediaView.setPreserveRatio(true);

        vFXPanel.setScene(scene);
    }

    public void initMediaPlayer(String path) {
        File videoFile = new File(path);
        if (videoFile.exists() && videoFile.isFile()) {
            media = new Media(videoFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnEndOfMedia(() -> {
                if (GuiManager.getInstance().isUploaded()) {
                    GuiManager.getInstance().getVideoPanel().getMediaPlayer().stop();
                    GuiManager.getInstance().getControlPanel().applyStopButtonStatus();
                    return;
                }

                // Send TEARDOWN
                RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
                if (rtspUnit == null) {
                    return;
                }

                StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
                RtspNettyChannel rtspNettyChannel = NettyChannelManager.getInstance().getRtspChannel(rtspUnit.getRtspUnitId());
                if (rtspNettyChannel != null) {
                    rtspNettyChannel.sendStop(rtspUnit);
                } else {
                    logger.warn("({}) Rtsp Channel is closed. Fail to process STOP.", rtspUnit.getRtspUnitId());
                    if (rtspStateHandler != null) {
                        rtspStateHandler.fire(
                                RtspEvent.IDLE,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );
                    }
                }
            });
            mediaView.setMediaPlayer(mediaPlayer);
            logger.debug("Success to init media player. (path={})", path);
        } else {
            logger.warn("Fail to init media player. Video file is not exist or not file. (path={})", path);
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}
