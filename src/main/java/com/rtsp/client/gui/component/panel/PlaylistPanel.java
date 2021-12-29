package com.rtsp.client.gui.component.panel;

import com.rtsp.client.file.base.PlaylistManager;
import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.service.AppInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;

public class PlaylistPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(PlaylistPanel.class);

    private final PlaylistManager playlistManager;

    private final JLabel playlistName = new JLabel("재생 목록");
    private final JList playlistView = new JList();
    private final DefaultListModel model = new DefaultListModel();

    private String selectedUri = null;

    public PlaylistPanel() {

        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setVgap(3);
        borderLayout.setHgap(3);
        setLayout(borderLayout);

        playlistManager = new PlaylistManager(AppInstance.getInstance().getConfigManager().getPlaylistSize());
        playlistName.setHorizontalAlignment(JLabel.CENTER);
        playlistName.setPreferredSize(new Dimension(this.getWidth(), 20));
        this.add(playlistName, BorderLayout.NORTH);
        initPlaylistView();
        loadPlaylist();
    }

    private void initPlaylistView() {
        playlistView.setModel(model);
        playlistView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlistView.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // TODO : 창 하나 조그만거 띄워서 삭제 > 삭제 버튼 (remove)
                    int index = playlistView.locationToIndex(e.getPoint());
                    playlistView.setSelectedIndex(index);
                    selectedUri = (String) model.get(index);
                    int isDelete = JOptionPane.showOptionDialog(null, selectedUri, "URI Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[]{"YES", "NO"}, "YES");

                    if (isDelete == 0) {
                        removePlaylist(selectedUri);
                    }
                } else {
                    int index = playlistView.locationToIndex(e.getPoint());
                    playlistView.setSelectedIndex(index);
                    selectedUri = (String) model.get(index);

                    selectPlaylist();
                }
            }
        });

        this.add(new JScrollPane(playlistView), BorderLayout.CENTER);
    }

    private void loadPlaylist() {
        HashMap<Integer, String> playlist = (HashMap<Integer, String>) playlistManager.startPlaylist();
        if (!playlist.isEmpty()) {
            playlist.forEach((key, value) -> model.add(key, value));
        }
    }

    public void savePlaylist() {
        playlistManager.stopPlaylist();
    }

    public void addPlaylist(int index, String uri) {
        playlistManager.addPlaylist(index, uri);
        model.clear();
        playlistManager.getPlaylistMap().forEach((key, value) -> model.add(key, value));
    }

    public void removePlaylist(String uri) {
        playlistManager.removePlaylist(uri);
        model.clear();
        playlistManager.getPlaylistMap().forEach((key, value) -> model.add(key, value));
    }

    public void selectPlaylist() {
        GuiManager.getInstance().getUriPanel().getUriTextField().setText(selectedUri);
    }

    public String getSelectedUri() {
        return selectedUri;
    }
}
