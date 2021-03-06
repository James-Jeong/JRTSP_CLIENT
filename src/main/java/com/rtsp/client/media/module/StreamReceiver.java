package com.rtsp.client.media.module;

import com.rtsp.client.ffmpeg.FfmpegManager;
import com.rtsp.client.file.RtspFileManager;
import com.rtsp.client.file.base.FileStream;
import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.media.netty.module.RtspManager;
import com.rtsp.client.media.netty.module.base.RtspUnit;
import com.rtsp.client.service.AppInstance;
import com.rtsp.client.service.ServiceManager;
import com.rtsp.client.service.scheduler.job.Job;
import com.rtsp.client.service.scheduler.schedule.ScheduleManager;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import org.apache.commons.lang3.time.StopWatch;
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

    private final FfmpegManager ffmpegManager = new FfmpegManager();
    private int totalRecvTsBytes = 0;

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

        if (rtspUnit.isPaused()) {
            return;
        }

        if (!rtspUnit.isStarted()) {
            rtspUnit.setStarted(true);
            StopWatch rtpTimeoutStopWatch = rtspUnit.getRtpTimeoutStopWatch();
            rtpTimeoutStopWatch.start();
        }

        // ????????? ???????????? ???????????? ???????????? ????????? ??????????????? M3U8 ?????? ?????? ??????
        RtspFileManager fileManager = rtspUnit.getFileManager();
        List<MediaSegment> mediaSegmentList = rtspUnit.getMediaSegmentList();
        if (mediaSegmentList == null || mediaSegmentList.isEmpty()) {
            // GET M3U8
            byte[] data = rtspUnit.pollFromM3U8Buffer();
            if (data == null || data.length <= 0) {
                return;
            }

            // M3U8 ?????? ??????(UDP, paylaod ??????), ????????? TS ?????? ?????????(RTP, payload 188)
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
                MediaPlaylistParser parser = new MediaPlaylistParser();
                MediaPlaylist playlist = parser.readPlaylist(Paths.get(m3u8FileName));
                if (playlist != null) {
                    mediaSegmentList = playlist.mediaSegments();
                    rtspUnit.setTsFileLimit(mediaSegmentList.size());
                    rtspUnit.setMediaSegmentList(mediaSegmentList);
                    logger.debug("({}) ({}) MediaSegmentList: {}", getName(), rtspUnit.getSessionId(), mediaSegmentList);
                }
            } catch (Exception e) {
                logger.warn("({}) ({}) Fail to get the media segment list. (m3u8FilePath={})", getName(), rtspUnit.getSessionId(), m3u8FileName, e);
            }
        } else {
            // GET TS
            byte[] data = rtspUnit.pollFromTsBuffer();
            if (data == null || data.length == 0) {
                // Convert M3U8 to MP4 by timeout
                long rtpTimeout = AppInstance.getInstance().getConfigManager().getRtpTimeout();
                if (rtpTimeout > 0) {
                    StopWatch rtpTimeoutStopWatch = rtspUnit.getRtpTimeoutStopWatch();
                    long curTime = rtpTimeoutStopWatch.getTime(TimeUnit.SECONDS);
                    if (curTime >= rtpTimeout) {
                        logger.debug("({}) ({}) >> RTP Timeout (curTime={}, limit={})", getName(), rtspUnit.getSessionId(), curTime, rtpTimeout);
                        logger.debug("({}) ({}) >> Recv TS Bytes: {}", getName(), rtspUnit.getSessionId(), totalRecvTsBytes);
                        totalRecvTsBytes = 0;

                        rtpTimeoutStopWatch.reset();
                        rtspUnit.setStarted(false);

                        ffmpegManager.convertM3u8ToMp4(rtspUnit.getM3u8FilePath(), rtspUnit.getMp4FilePath());
                        GuiManager.getInstance().getVideoPanel().initMediaPlayer(rtspUnit.getMp4FilePath());
                        GuiManager.getInstance().getVideoPanel().getMediaPlayer().play();
                        fileManager.resetTsFileIndex();
                        rtspUnit.setMediaSegmentList(null);
                        ServiceManager.getInstance().getScheduleManager().stopJob(RtspUnit.VIDEO_JOB_KEY, this);
                        return;
                    }
                }
                return;
            }

            String dataStr = new String(data, StandardCharsets.UTF_8);

            // ?????? TS ?????? ??? ?????? ????????? ?????? TS ????????? ???????????? ????????????
            // > FFmpeg ??? ????????? TS ???????????? ?????? ????????? ??? ?????? [~FFmpeg Service~] ??? ????????????.
            String tsFileName = rtspUnit.getTsFilePath();
            int tsFileIndex = fileManager.getTsFileIndex();
            String curTsFileName = String.format(tsFileName, tsFileIndex);

            // ?????? ???????????? TS ?????? ?????? ?????? > ?????? ????????? ???????????? ??????
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

            // ?????? TS ?????? ???????????? 0 ?????? ??????, ?????? ?????? ???????????? FFMPEG ???????????? ?????????, ?????? TS ????????? ????????? ??????
            int tsFileSize = fileManager.getTsFileSize(tsFileIndex);
            if (tsFileSize > 0 && dataStr.contains(FFMPEG_TS_FILE_HEADER)) {
                if (!fileManager.closeTsFile(tsFileIndex)) {
                    logger.warn("({}) ({}) Fail to close the file stream. (path={})", getName(), rtspUnit.getSessionId(), tsFileStream.getFilePath());
                }
                totalRecvTsBytes += tsFileSize;
                logger.debug("({}) ({}) Success to write completely the ts file. (index={}, size={}, path={})", getName(), rtspUnit.getSessionId(), tsFileIndex, tsFileSize, tsFileStream.getFilePath());
                //logger.debug("({}) ({}) Total time: {}", getName(), rtspUnit.getSessionId(), ffmpegManager.getFileTime(curTsFileName));

                // ??????????????? ???????????? TS ?????? ??????
                // ????????? ???????????? ??????????????? ???????????? ????????? ????????? ?????? > ????????? TS ??? ?????? ??????
                tsFileIndex = fileManager.addAndGetTsFileIndex();
                if (tsFileIndex >= rtspUnit.getTsFileLimit()) {
                    logger.warn("Fail to process the ts file. Index is exceeded. (index={}, mediaSegmentListSize={})", tsFileIndex, mediaSegmentList.size());
                    return;
                }

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
