package com.rtsp.client.file.base;

import com.rtsp.client.file.PlaylistFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistManager {

    private static final Logger log = LoggerFactory.getLogger(PlaylistManager.class);

    private static final String RECENT_PLAYLIST_PATH = System.getProperty("user.dir") + "/src/main/resources/playlist/";
    private static final String RECENT_PLAYLIST_FILE_PATH = RECENT_PLAYLIST_PATH + "recent.playlist";

    private final PlaylistFileManager playListFileManager = new PlaylistFileManager();

    private final HashMap<Integer, String> playlistMap;

    private final int playlistMaxSize;

    public PlaylistManager(int playlistMaxSize) {
        this.playlistMap = new HashMap<>();
        this.playlistMaxSize = playlistMaxSize;
        playListFileManager.createPlaylistFile(RECENT_PLAYLIST_FILE_PATH);
    }

    public void stopPlaylist() {
        playListFileManager.openPlaylistFile();

        StringBuilder stringBuilder = new StringBuilder();

        if (playlistMap.isEmpty()) {
            return;
        }
        playlistMap.keySet().stream().sorted().forEach(key -> stringBuilder.append(playlistMap.get(key)+"\n") );
        playListFileManager.writePlaylistFile(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));

        playListFileManager.closePlaylistFile();
    }


    public Map<Integer, String> startPlaylist() {
        List<String> playlist = playListFileManager.readPlaylistFile();

        if (playlist.isEmpty()) {
            return playlistMap;
        }

        playlistMap.clear();
        int index = 0;
        for (String value : playlist) {
            if (index >= playlistMaxSize) {
                break;
            }
            playlistMap.put(index++, value);
        }

        return playlistMap;
    }

    public void addPlaylist(int index, String data){
        if (index >= playlistMaxSize || index < 0 || data.length() == 0) {
            return;
        }

        // 기존 리스트에 이미 존재하는 경우
        if (playlistMap.containsValue(data)) {
            if (playlistMap.size() <= index) {
                index = playlistMap.size() - 1;
            }

            int oldIndex = index;
            for (Map.Entry<Integer, String> entry : playlistMap.entrySet()) {
                if (entry.getValue().equals(data)) {
                    if (entry.getKey() == oldIndex) {
                        return;
                    }
                    oldIndex = entry.getKey();
                    break;
                }
            }

            // 아래에서 위로 옮기는 경우
            if (index < oldIndex) {
                for (int key = oldIndex; key > index; key--) {
                    playlistMap.replace(key, playlistMap.get(key - 1));
                }
            }
            // 위에서 아래로 옮기는 경우
            else if (index > oldIndex) {
                for (int key = oldIndex; key < index; key++) {
                    playlistMap.replace(key, playlistMap.get(key + 1));
                }
            }
            playlistMap.replace(index, data);
        }
        // 새로운 data 인 경우
        else {
            if (playlistMap.size() < index) {
                index = playlistMap.size();
            }

            if (playlistMap.size() == index) {
                playlistMap.put(index, data);
            } else {
                if (playlistMap.size() < playlistMaxSize) {
                    playlistMap.put(playlistMap.size(), playlistMap.get(playlistMap.size()));
                }
                for (int key = playlistMap.size() - 1; key > index; key--) {
                    playlistMap.replace(key, playlistMap.get(key - 1));
                }
                playlistMap.replace(index, data);
            }
        }
    }

    public void removePlaylist(String data) {
        if (data.length() == 0 || !playlistMap.containsValue(data)) {
            return;
        }

        int index = -1;
        for (Map.Entry<Integer, String> entry : playlistMap.entrySet()) {
            if (entry.getValue().equals(data)) {
                index = entry.getKey();
                break;
            }
        }

        for (int key = index; key < playlistMap.size(); key++) {
            if (key == playlistMap.size()-1) {
                playlistMap.remove(key);
            } else {
                playlistMap.replace(key, playlistMap.get(key + 1));
            }
        }
    }

    public Map<Integer, String> getPlaylistMap() {
        return playlistMap;
    }

    public int getPlaylistMapSize() {
        return playlistMap.size();
    }
}
