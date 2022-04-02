package com.lifeofcoder.autolimiter.cluster.server.connection;

import com.lifeofcoder.autolimiter.cluster.server.config.ClusterServerConfigManager;
import com.lifeofcoder.autolimiter.cluster.server.config.ServerTransportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author xuyue
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ScanIdleConnectionTask implements Runnable {
    private static Logger LOGGER = LoggerFactory.getLogger(ScanIdleConnectionTask.class);

    private final ConnectionPool connectionPool;

    public ScanIdleConnectionTask(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public void run() {
        try {
            int idleSeconds = ClusterServerConfigManager.getIdleSeconds();
            long idleTimeMillis = idleSeconds * 1000;
            if (idleTimeMillis < 0) {
                idleTimeMillis = ServerTransportConfig.DEFAULT_IDLE_SECONDS * 1000;
            }
            long now = System.currentTimeMillis();
            List<Connection> connections = connectionPool.listAllConnection();
            for (Connection conn : connections) {
                if ((now - conn.getLastReadTime()) > idleTimeMillis) {
                    LOGGER.info(String.format("[ScanIdleConnectionTask] The connection <%s:%d> has been idle for <%d>s. " + "It will be closed now.", conn.getRemoteIP(), conn.getRemotePort(), idleSeconds));
                    conn.close();
                }
            }
        }
        catch (Throwable t) {
            LOGGER.warn("[ScanIdleConnectionTask] Failed to clean-up idle tasks", t);
        }
    }
}
