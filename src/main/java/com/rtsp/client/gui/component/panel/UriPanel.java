package com.rtsp.client.gui.component.panel;

import com.rtsp.client.gui.base.ButtonType;
import com.rtsp.client.gui.buttonlistener.addPlaylistButtonListener;
import com.rtsp.client.gui.util.TextFieldLimit;
import com.rtsp.client.service.AppInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class UriPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(UriPanel.class);

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
        JPopupMenu popup = new JPopupMenu();
        //UndoManager undoManager = new UndoManager();

        Action copyAction = new AbstractAction("Copy") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                uriTextField.copy();
            }
        };

        Action cutAction = new AbstractAction("Cut") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                uriTextField.cut();
            }
        };

        Action pasteAction = new AbstractAction("Paste") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                uriTextField.paste();
            }
        };

        Action selectAllAction = new AbstractAction("Select All") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                uriTextField.selectAll();
            }
        };

        // UNDO 는 잘 안됨
        /*uriTextField.getDocument().addUndoableEditListener(undoManager);
        uriTextField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK), "undo");
        uriTextField.getActionMap().put("undo", new TextAction("undo") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });*/

        cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
        copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
        pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
        selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));

        popup.addSeparator();
        popup.add (cutAction);
        popup.add (copyAction);
        popup.add (pasteAction);
        popup.addSeparator();
        popup.add (selectAllAction);

        uriTextField.setComponentPopupMenu(popup);
        uriTextField.setDocument(new TextFieldLimit(AppInstance.getInstance().getConfigManager().getUriLimit()));
    }

    private void initButton() {
        addPlaylistButton.addActionListener(new addPlaylistButtonListener());
    }

    public JTextField getUriTextField() {
        return uriTextField;
    }
}
