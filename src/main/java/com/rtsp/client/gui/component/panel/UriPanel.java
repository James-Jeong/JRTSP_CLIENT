package com.rtsp.client.gui.component.panel;

import com.rtsp.client.gui.util.TextFieldLimit;
import com.rtsp.client.service.AppInstance;

import javax.swing.*;
import java.awt.*;

public class UriPanel extends JPanel {

    private static final JLabel URI_LABEL = new JLabel("  URI : ");
    private final JTextField uriTextField = new JTextField(1);

    public UriPanel() {
        this.setLayout(new BorderLayout());

        initTextField();

        this.add(URI_LABEL, BorderLayout.WEST);
        this.add(uriTextField, BorderLayout.CENTER);
    }

    private void initTextField() {
        uriTextField.setDocument(new TextFieldLimit(AppInstance.getInstance().getConfigManager().getUriLimit()));
    }

    public JTextField getUriTextField() {
        return uriTextField;
    }
}
