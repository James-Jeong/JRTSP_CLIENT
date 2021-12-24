package com.rtsp.client.file.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.locks.ReentrantLock;

public class FileStream {

    private static final Logger logger = LoggerFactory.getLogger(FileStream.class);

    private final String filePath;
    private File ramFile;
    private FileOutputStream fileOutputStream;
    private final ReentrantLock fileStreamLock = new ReentrantLock();
    private boolean isQuit = false;

    private final int limitDataSize;
    private int totalDataSize = 0;

    ////////////////////////////////////////////////////////////////////////////////

    public FileStream(String filePath, int limitDataSize) {
        this.filePath = filePath;
        this.limitDataSize = limitDataSize;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void createFile(boolean isDelete) {
        if (ramFile != null) {
            return;
        }

        ramFile = new File(filePath);
        if (ramFile.isDirectory()) {
            logger.warn("Fail to open fileStream. File name is path. ({})", filePath);
        } else {
            if (isDelete) {
                // 파일 append 를 막기 위해 기존 파일을 지운다.
                removeFile();
            }
        }
    }

    public File getFile() {
        return ramFile;
    }

    public void removeFile() {
        if (ramFile == null || !closeFileStream()) {
            return;
        }

        try {
            if (ramFile.exists()) {
                if (ramFile.delete()) {
                    logger.debug("Success to remove the file. ({})", filePath);
                } else {
                    logger.warn("Fail to remove the file. ({})", filePath);
                }

                ramFile = null;
                totalDataSize = 0;
            } else {
                logger.warn("Fail to remove the file. Not exists. ({})", filePath);
            }
        } catch (Exception e) {
            logger.warn("Fail to remove the file. (path={})", filePath, e);
        }
    }

    public boolean writeDataToFile(byte[] data) {
        return writeFileStream(data);
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isQuit() {
        return isQuit;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public boolean openFileStream(File ramFile, boolean append) {
        try {
            fileStreamLock.lock();

            if (fileOutputStream == null) {
                isQuit = false;
                fileOutputStream = new FileOutputStream(ramFile, append);
                //logger.trace("Success to open the fileStream. (path={})", ramFile.getName());
            }
        } catch (Exception e) {
            logger.warn("Fail to open the fileStream. (path={})", ramFile.getName(), e);
            return false;
        } finally {
            fileStreamLock.unlock();
        }

        return true;
    }

    public boolean closeFileStream() {
        if (fileOutputStream == null) {
            return false;
        }

        try {
            fileStreamLock.lock();

            if (fileOutputStream != null) {
                isQuit = true;
                fileOutputStream.flush();
                fileOutputStream.close();
                fileOutputStream = null;
                //logger.trace("FileStream is closed. (path={}, totalDataSize={})", filePath, totalDataSize);
            }
        } catch (Exception e) {
            logger.warn("Fail to close the fileStream. (path={})", fileStreamLock, e);
            return false;
        } finally {
            fileStreamLock.unlock();
        }

        return true;
    }

    public boolean writeFileStream (byte[] data) {
        if (ramFile == null || fileOutputStream == null) {
            return false;
        }

        if (isQuit) {
            closeFileStream();
            return false;
        }

        try {
            fileStreamLock.lock();

            fileOutputStream.write(data);
            totalDataSize += data.length;
        } catch (Exception e) {
            logger.warn("Fail to write media data. (path={})", filePath, e);
            return false;
        } finally {
            fileStreamLock.unlock();
        }

        if (limitDataSize > 0 && (limitDataSize >= totalDataSize)) {
            closeFileStream();
        }

        return true;
    }

    public int getTotalDataSize() {
        return totalDataSize;
    }

}
