package com.rtsp.client.media.module;

import com.rtsp.client.file.FileManager;
import com.rtsp.client.file.base.FileStream;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.service.scheduler.job.Job;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StreamReceiver extends Job {

    private static final Logger logger = LoggerFactory.getLogger(StreamReceiver.class);

    public static final String M3U8_FILE_HEADER = "#EXTM3U";
    public static final String FFMPEG_TS_FILE_HEADER = "FFmpeg";
    //public static final byte[] LAST_TS_FILE_HEAD = { 0x47, 0x01, 0x01, 0x3D };
    //public static final byte[] LAST_TS_FILE_TAIL = { (byte) 0xF1, 0x4C, (byte) 0x80, 0x01, (byte) 0xBF, (byte) 0xFC, 0x21, 0x10, 0x04, 0x60, (byte) 0x8C, 0x1C };

    ////////////////////////////////////////////////////////////////////////////////

    public StreamReceiver(String name, int initialDelay, int interval, TimeUnit timeUnit, int priority, int totalRunCount, boolean isLasted) {
        super(name, initialDelay, interval, timeUnit, priority, totalRunCount, isLasted);
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void run() {
        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit == null) {
            return;
        }

        byte[] data = rtspUnit.poll();
        if (data == null || data.length <= 0) {
            return;
        }

        // M3U8 먼저 도착(UDP, paylaod 가변), 그리고 TS 파일 수신함(RTP, payload 188)
        FileManager fileManager = rtspUnit.getFileManager();
        String dataStr = new String(data, StandardCharsets.UTF_8);

        // GET M3U8
        if (dataStr.startsWith(M3U8_FILE_HEADER)) {
            logger.debug("({}) ({}) >> Recv M3U8\n{}(size={})",
                    getName(), rtspUnit.getSessionId(),
                    new String(data, StandardCharsets.UTF_8), data.length
            );

            String m3u8FileName = rtspUnit.getM3u8FilePath();
            fileManager.createM3U8File(m3u8FileName);
            if (!fileManager.openM3U8File()) {
                logger.warn("({}) ({}) Fail to open the m3u8 file. (path={})", getName(), rtspUnit.getSessionId(), m3u8FileName);
                fileManager.removeM3U8File();
                return;
            } else {
                logger.trace("({}) ({}) Success to open the m3u8 file. (path={})", getName(), rtspUnit.getSessionId(), m3u8FileName);
            }

            if (fileManager.writeM3U8File(data)) {
                logger.debug("({}) ({}) Success to write the data into m3u8 file. (m3u8FileName={})", getName(), rtspUnit.getSessionId(), m3u8FileName);
            }

            if (!fileManager.closeM3U8File()) {
                logger.warn("({}) ({}) Fail to close the m3u8 file. (path={})", getName(), rtspUnit.getSessionId(), m3u8FileName);
            } else {
                logger.trace("({}) ({}) Success to close the m3u8 file. (path={})", getName(), rtspUnit.getSessionId(), m3u8FileName);
            }

            try {
                List<MediaSegment> mediaSegmentList;
                MediaPlaylistParser parser = new MediaPlaylistParser();
                MediaPlaylist playlist = parser.readPlaylist(Paths.get(m3u8FileName));
                if (playlist != null) {
                    mediaSegmentList = playlist.mediaSegments();
                    rtspUnit.setTsFileLimit(mediaSegmentList.size());
                }
            } catch (Exception e) {
                logger.warn("({}) ({}) Fail to get the media segment list. (m3u8FilePath={})", getName(), rtspUnit.getSessionId(), m3u8FileName, e);
            }
        }
        // GET TS
        else {
            // 특정 TS 파일 다 쌓을 때까지 다음 TS 파일로 넘어가지 않아야함
            // > FFmpeg 가 생성한 TS 파일에는 각각 데이터 맨 앞에 [~FFmpeg Service~] 가 추가된다.
            String tsFileName = rtspUnit.getTsFilePath();
            int tsFileIndex = fileManager.getTsFileIndex();
            String curTsFileName = String.format(tsFileName, tsFileIndex);

            // 현재 인덱스로 TS 파일 생성 시도 > 이미 있으면 생성하지 않음
            FileStream tsFileStream = fileManager.createTsFile(tsFileIndex, curTsFileName, 0);
            if (tsFileStream == null) {
                logger.warn("({}) ({}) Fail to create the ts file. (index={})", getName(), rtspUnit.getSessionId(), tsFileIndex);
                return;
            }

            if (!fileManager.openTsFile(tsFileIndex)) {
                logger.warn("({}) ({}) Fail to open the ts file. (path={})", getName(), rtspUnit.getSessionId(), tsFileStream.getFilePath());
                fileManager.removeTsFile(tsFileIndex);
                return;
            }

            // 현재 TS 파일 사이즈가 0 보다 크고, 현재 패킷 데이터에 FFMPEG 문자열이 온다면, 다음 TS 파일에 데이터 적재
            if (fileManager.getTsFileSize(tsFileIndex) > 0 && dataStr.contains(FFMPEG_TS_FILE_HEADER)) {
                if (!fileManager.closeTsFile(tsFileIndex)) {
                    logger.warn("({}) ({}) Fail to close the file stream. (path={})", getName(), rtspUnit.getSessionId(), tsFileStream.getFilePath());
                }
                logger.debug("({}) ({}) Success to write completely the ts file. (index={}, path={})", getName(), rtspUnit.getSessionId(), tsFileIndex, tsFileStream.getFilePath());

                // 업데이트된 인덱스로 TS 파일 생성
                tsFileIndex = fileManager.addAndGetTsFileIndex();
                logger.debug("({}) ({}) The index of the next ts file is defined. ({})", getName(), rtspUnit.getSessionId(), tsFileIndex);
                curTsFileName = String.format(tsFileName, tsFileIndex);
                tsFileStream = fileManager.createTsFile(tsFileIndex, curTsFileName, 0);
                if (tsFileStream == null) {
                    logger.warn("({}) ({}) Fail to create the ts file. (index={})", getName(), rtspUnit.getSessionId(), tsFileIndex);
                    return;
                }

                if (!fileManager.openTsFile(tsFileIndex)) {
                    logger.warn("({}) ({}) Fail to open the ts file. (path={})", getName(), rtspUnit.getSessionId(), tsFileStream.getFilePath());
                    fileManager.removeTsFile(tsFileIndex);
                    return;
                }
            }

            if (fileManager.writeDataToTsFile(tsFileIndex, data)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("({}) ({}) Success to write the data into ts file. (tsFileIndex={}, tsFileName={})", getName(), rtspUnit.getSessionId(), tsFileIndex, tsFileName);
                }
            }

            if (!fileManager.closeTsFile(tsFileIndex)) {
                logger.warn("({}) ({}) Fail to close the file stream. (path={})", getName(), rtspUnit.getSessionId(), tsFileStream.getFilePath());
            }
        }
    }
}
