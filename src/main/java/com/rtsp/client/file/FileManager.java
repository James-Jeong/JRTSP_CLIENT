package com.rtsp.client.file;

import com.rtsp.client.file.base.FileStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class FileManager {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    private final String rtspUnitId;

    private FileStream m3u8File = null;
    private final ReentrantLock m3u8FileLock = new ReentrantLock();

    private final LinkedHashMap<Integer, FileStream> tsFileList = new LinkedHashMap<>();
    private final ReentrantLock tsFileListLock = new ReentrantLock();
    private final AtomicInteger tsFileIndex = new AtomicInteger(0);

    ////////////////////////////////////////////////////////////////////////////////

    public FileManager(String rtspUnitId) {
        this.rtspUnitId = rtspUnitId;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void createM3U8File(String m3u8FilePath) {
        if (m3u8FilePath == null || m3u8FilePath.length() == 0 || m3u8File != null) { return; }

        m3u8FileLock.lock();
        try {
            m3u8File = new FileStream(m3u8FilePath, 0);
            m3u8File.createFile(true);
        } catch (Exception e) {
            logger.warn("({}) Fail to create the m3u8 file. (path={})", m3u8FilePath, e);
        } finally {
            m3u8FileLock.unlock();
        }
    }

    public boolean openM3U8File() {
        if (m3u8File == null) {
            return false;
        }

        return m3u8File.openFileStream(m3u8File.getFile(), true);
    }

    public boolean closeM3U8File() {
        if (m3u8File == null) {
            return false;
        }

        return m3u8File.closeFileStream();
    }

    public void removeM3U8File() {
        if (m3u8File == null) {
            return;
        }

        m3u8FileLock.lock();
        try {
            m3u8File.removeFile();
            m3u8File = null;
        } catch (Exception e) {
            logger.warn("({}) Fail to remove the m3u8 file. (path={})", rtspUnitId, e);
        } finally {
            m3u8FileLock.unlock();
        }
    }

    public boolean writeM3U8File(byte[] data) {
        if (data == null || data.length == 0 || m3u8File == null) { return false; }

        m3u8FileLock.lock();
        try {
            return m3u8File.writeFileStream(data);
        } catch (Exception e) {
            logger.warn("({}) Fail to write the m3u8 file. (path={})", m3u8File.getFilePath(), e);
            return false;
        } finally {
            m3u8FileLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public FileStream createTsFile(int tsFileIndex, String tsFilePath, int limitDataSize) {
        if (tsFilePath == null || tsFilePath.length() == 0) { return null; }

        return addTsFilePathToList(tsFileIndex, tsFilePath, limitDataSize);
    }

    public void removeTsFile(int tsFileIndex) {
        removeTsFilePathFromList(tsFileIndex);
    }

    public boolean openTsFile(int tsFileIndex) {
        FileStream fileStream = getTsFileStreamFromList(tsFileIndex);
        if (fileStream == null) {
            logger.warn("({}) Fail to close the tsFilePath. Not exists. (index={})", rtspUnitId, tsFileIndex);
            return false;
        }

        return fileStream.openFileStream(fileStream.getFile(), true);
    }

    public boolean closeTsFile(int tsFileIndex) {
        FileStream fileStream = getTsFileStreamFromList(tsFileIndex);
        if (fileStream == null) {
            logger.warn("({}) Fail to close the tsFilePath. Not exists. (index={})", rtspUnitId, tsFileIndex);
            return false;
        }

        return fileStream.closeFileStream();
    }

    public void removeAllTsFiles() {
        removeAllTsFilePathsFromList();
    }

    public boolean writeDataToTsFile(int tsFileIndex, byte[] data) {
        if (data == null || data.length == 0) { return false; }

        FileStream fileStream = getTsFileStreamFromList(tsFileIndex);
        if (fileStream == null) {
            logger.warn("({}) Fail to write the ts file. Index is not defined. (index={})", rtspUnitId, tsFileIndex);
            return false;
        }

        return fileStream.writeFileStream(data);
    }

    public int addAndGetTsFileIndex() {
        return tsFileIndex.incrementAndGet();
    }

    public int getTsFileIndex() {
        return tsFileIndex.get();
    }

    public int getTsFileSize(int tsFileIndex) {
        FileStream fileStream = getTsFileStreamFromList(tsFileIndex);
        if (fileStream == null) {
            logger.warn("({}) Fail to get the ts file size. Not exists. (index={})", rtspUnitId, tsFileIndex);
            return 0;
        }

        return fileStream.getTotalDataSize();
    }

    ////////////////////////////////////////////////////////////////////////////////

    private FileStream addTsFilePathToList(int tsFileIndex, String tsFilePath, int limitDataSize) {
        if (tsFilePath == null || tsFilePath.length() == 0) { return null; }

        FileStream tsFileStream = getTsFileStreamFromList(tsFileIndex);
        if (tsFileStream != null) {
            //logger.warn("({}) Fail to put the tsFilePath. Already exists. (index={}, path={})", rtspUnitId, tsFileIndex, tsFilePath);
            return tsFileStream;
        }

        tsFileListLock.lock();
        try {
            tsFileStream = new FileStream(tsFilePath, limitDataSize);
            tsFileStream.createFile(true);
            tsFileList.put(tsFileIndex, tsFileStream);
        } catch (Exception e) {
            logger.warn("({}) Fail to put the tsFilePath. (index={}, path={})", rtspUnitId, tsFileIndex, tsFilePath, e);
            return null;
        } finally {
            tsFileListLock.unlock();
        }

        return tsFileStream;
    }

    private void removeTsFilePathFromList(int tsFileIndex) {
        FileStream fileStream = getTsFileStreamFromList(tsFileIndex);
        if (fileStream == null) {
            logger.warn("({}) Fail to remove the tsFilePath. Not exists. (index={})", rtspUnitId, tsFileIndex);
            return;
        }

        tsFileListLock.lock();
        try {
            fileStream.removeFile();
            tsFileList.remove(tsFileIndex);
        } catch (Exception e) {
            logger.warn("({}) Fail to remove the tsFilePath. (index={})", rtspUnitId, tsFileIndex, e);
        } finally {
            tsFileListLock.unlock();
        }
    }

    private void removeAllTsFilePathsFromList() {
        Set<Map.Entry<Integer, FileStream>> entrySet = tsFileList.entrySet();
        if (entrySet.isEmpty()) {
            return;
        }

        tsFileListLock.lock();
        try {
            for (Map.Entry<Integer, FileStream> entry : entrySet) {
                if (entry == null) {
                    continue;
                }

                FileStream fileStream = entry.getValue();
                if (fileStream == null) {
                    logger.warn("({}) Fail to remove the ts file. Index is not defined. (index={})", rtspUnitId, tsFileIndex);
                    continue;
                }

                fileStream.removeFile();
                tsFileList.remove(entry.getKey());
            }

            tsFileIndex.set(0);
        } catch (Exception e) {
            logger.warn("({}) Fail to remove the tsFilePath. (index={})", rtspUnitId, tsFileIndex, e);
        } finally {
            tsFileListLock.unlock();
        }
    }

    private FileStream getTsFileStreamFromList(int tsFileIndex) {
        return tsFileList.get(tsFileIndex);
    }

    ////////////////////////////////////////////////////////////////////////////////

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.length() == 0) { return null; }

        int extensionPos = fileName.lastIndexOf(".");
        if (extensionPos == -1) { return null; }
        return fileName.substring(extensionPos + 1);
    }

    public String getFileNameExceptForExtension(String fullFileName) {
        if (fullFileName == null || fullFileName.length() == 0) { return null; }

        // 파일 경로에 [.] 이나 [/] 문자가 없으면 파일 경로 그대로 반환
        if (!fullFileName.contains(".") || !fullFileName.contains("/")) {
            return fullFileName;
        }

        return fullFileName.substring(fullFileName.lastIndexOf("/"), fullFileName.lastIndexOf("."));
    }

}
