package com.rtsp.client.gui.component.panel;

import com.rtsp.client.gui.base.ButtonType;
import com.rtsp.client.gui.buttonlistener.addPlaylistButtonListener;
import com.rtsp.client.gui.util.TextFieldLimit;
import com.rtsp.client.service.AppInstance;

import javax.swing.*;
import java.awt.*;

public class UriPanel extends JPanel {

    private static final JLabel URI_LABEL = new JLabel("  URI : ");

    private final JTextField uriTextField = new JTextField(1);
    private final JButton addPlaylistButton = new JButton(ButtonType.ADD);

    public UriPanel() {
        this.setLayout(new BorderLayout());

        initTextField();
        initButton();

        this.add(URI_LABEL, BorderLayout.WEST);
        this.add(uriTextField, BorderLayout.CENTER);
        this.add(addPlaylistButton, BorderLayout.EAST);
    }

    private void initTextField() {
        uriTextField.setDocument(new TextFieldLimit(AppInstance.getInstance().getConfigManager().getUriLimit()));
    }

    private void initButton() {
        addPlaylistButton.addActionListener(new addPlaylistButtonListener());
    }

    public JTextField getUriTextField() {
        return uriTextField;
    }
}
