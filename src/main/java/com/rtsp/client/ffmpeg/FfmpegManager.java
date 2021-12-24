package com.rtsp.client.ffmpeg;

import com.rtsp.client.config.ConfigManager;
import com.rtsp.client.service.AppInstance;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.mp4parser.IsoFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @class public class FfmpegManager
 * @brief FfmpegManager class
 */
public class FfmpegManager {

    private static final Logger logger = LoggerFactory.getLogger(FfmpegManager.class);

    private static final String BITSTREAM_FILTER = "aac_adtstoasc";
    private static final String C_CODEC = "copy";

    //public static final String FFMPEG_TAG = "ffmpeg";

    private FFmpeg ffmpeg = null;
    private FFprobe ffprobe = null;
    private FFmpegExecutor executor = null;

    ////////////////////////////////////////////////////////////////////////////////

    public FfmpegManager() {
        //Nothing
    }

    public double getFileTime(String srcFilePath) {
        try {
            IsoFile isoFile = new IsoFile(srcFilePath);
            return (double) isoFile.getMovieBox().getMovieHeaderBox().getDuration() / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
        } catch (Exception e) {
            logger.warn("Fail to get the file time. (srcFilePath={})", srcFilePath);
            return 0;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void convertM3u8ToMp4(String srcFilePath, String destFilePath) {
        String destFilePathOnly = destFilePath.substring(
                0,
                destFilePath.lastIndexOf("/")
        );

        File destFilePathOnlyFile = new File(destFilePathOnly);
        if (destFilePathOnlyFile.mkdirs()) {
            logger.debug("Success to make the directory. ({})", destFilePathOnly);
        }

        try {
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            if (ffmpeg == null) {
                ffmpeg = new FFmpeg(configManager.getFfmpegPath());
            }

            if (ffprobe == null) {
                ffprobe = new FFprobe(configManager.getFfprobePath());
            }

            FFmpegBuilder builder = new FFmpegBuilder()
                    .overrideOutputFiles(true)
                    .setInput(srcFilePath)
                    .addOutput(destFilePath)
                    .setFormat("mp4")

                    .addExtraArgs("-bsf:a", BITSTREAM_FILTER)
                    .addExtraArgs("-c", C_CODEC)
                    .done();

            if (executor == null) {
                executor = new FFmpegExecutor(ffmpeg, ffprobe);
            }
            executor.createJob(builder).run();
        } catch (Exception e) {
            logger.error("FfmpegManager.convertM3u8ToMp4.Exception ", e);
        }
    }

    public void convertTsToMp4(String srcFilePath, String destFilePath) {
        String destFilePathOnly = destFilePath.substring(
                0,
                destFilePath.lastIndexOf("/")
        );

        File destFilePathOnlyFile = new File(destFilePathOnly);
        if (destFilePathOnlyFile.mkdirs()) {
            logger.debug("Success to make the directory. ({})", destFilePathOnly);
        }

        try {
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            if (ffmpeg == null) {
                ffmpeg = new FFmpeg(configManager.getFfmpegPath());
            }

            if (ffprobe == null) {
                ffprobe = new FFprobe(configManager.getFfprobePath());
            }

            FFmpegBuilder builder = new FFmpegBuilder()
                    .overrideOutputFiles(true)
                    .setInput(srcFilePath)
                    .addOutput(destFilePath)
                    .setFormat("mp4")

                    .addExtraArgs("-acodec", "copy")
                    .addExtraArgs("-vcodec", "copy")
                    .done();

            if (executor == null) {
                executor = new FFmpegExecutor(ffmpeg, ffprobe);
            }
            executor.createJob(builder).run();
        } catch (Exception e) {
            logger.error("FfmpegManager.convertTsToMp4>Exception ", e);
        }
    }

}
