package com.rtsp.client.service;

import com.rtsp.client.gui.GuiManager;
import com.rtsp.client.media.netty.NettyChannelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    private static ServiceManager serviceManager = null;
    private static final int DELAY = 1000;
    private boolean isQuit = false;

    ////////////////////////////////////////////////////////////////////////////////

    public ServiceManager() {
        Runtime.getRuntime().addShutdownHook(new ShutDownHookHandler("ShutDownHookHandler", Thread.currentThread()));

    }

    public static ServiceManager getInstance ( ) {
        if (serviceManager == null) {
            serviceManager = new ServiceManager();
        }

        return serviceManager;
    }


    ////////////////////////////////////////////////////////////////////////////////

    private void start() {
        logger.debug("| All services are opened.");

        // Add register channel
        NettyChannelManager.getInstance().addRegisterChannel();
    }

    public void stop() {

        GuiManager.getInstance().getPlaylistPanel().savePlaylist();
        // Remove register channel
        NettyChannelManager.getInstance().removeRegisterChannel();

        isQuit = true;
        logger.debug("| All services are closed.");
    }

    /**
     * @fn public void loop ()
     * @brief Main Service Loop
     */
    public void loop () {
        start();

        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        while (!isQuit) {
            try {
                timeUnit.sleep(DELAY);
            } catch (InterruptedException e) {
                logger.warn("| ServiceManager.loop.InterruptedException", e);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @class private static class ShutDownHookHandler extends Thread
     * @brief Graceful Shutdown 을 처리하는 클래스
     * Runtime.getRuntime().addShutdownHook(*) 에서 사용됨
     */
    private static class ShutDownHookHandler extends Thread {

        // shutdown 로직 후에 join 할 thread
        private final Thread target;

        public ShutDownHookHandler (String name, Thread target) {
            super(name);

            this.target = target;
            logger.debug("| ShutDownHookHandler is initiated. (target={})", target.getName());
        }

        /**
         * @fn public void run ()
         * @brief 정의된 Shutdown 로직을 수행하는 함수
         */
        @Override
        public void run ( ) {
            try {
                shutDown();
                target.join();
                logger.debug("| ShutDownHookHandler's target is finished successfully. (target={})", target.getName());
            } catch (Exception e) {
                logger.warn("| ShutDownHookHandler.run.Exception", e);
            }
        }

        /**
         * @fn private void shutDown ()
         * @brief Runtime 에서 선언된 Handler 에서 사용할 서비스 중지 함수
         */
        private void shutDown ( ) {
            logger.warn("| Process is about to quit. (Ctrl+C)");
            ServiceManager.getInstance().stop();
        }
    }

}
