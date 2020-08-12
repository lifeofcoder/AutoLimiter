package com.lifeofcoder.autolimiter.dashboard.model;

import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.discovery.MachineInfo;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author xbc
 * @date 2020/7/17
 */
public class IgniteMachine implements BaseIgniteModel, Binarylizable {
    private static Logger LOGGER = LoggerFactory.getLogger(IgniteMachine.class);

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_APP = "app";
    public static final String COLUMN_APP_TYPE = "app_type";
    public static final String COLUMN_IP = "ip";
    public static final String COLUMN_PORT = "port";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_LAST_HEARTBEAT = "last_heartbeat";
    public static final String COLUMN_HEARTBEAT_VERSION = "heartbeat_version";
    public static final String COLUMN_HOSTNAME = "hostname";

    private MachineInfo machineInfo;

    public IgniteMachine() {
    }

    public IgniteMachine(String app, String ip, int port) {
        this.machineInfo = new MachineInfo();
        this.machineInfo.setApp(app);
        this.machineInfo.setIp(ip);
        this.machineInfo.setPort(port);
    }

    public IgniteMachine(MachineInfo machineInfo) {
        this.machineInfo = machineInfo;
    }

    @Override
    public String key() {
        return machineInfo.getApp() + machineInfo.getIp() + machineInfo.getPort();
    }

    @Override
    public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        LOGGER.debug("Writer is executing...");
        writer.writeString(COLUMN_ID, key().toString());
        writer.writeString(COLUMN_APP, machineInfo.getApp());
        writer.writeInt(COLUMN_APP_TYPE, intValue(machineInfo.getAppType()));
        writer.writeString(COLUMN_IP, machineInfo.getIp());
        writer.writeInt(COLUMN_PORT, intValue(machineInfo.getPort()));
        writer.writeString(COLUMN_VERSION, machineInfo.getVersion());
        writer.writeLong(COLUMN_LAST_HEARTBEAT, machineInfo.getLastHeartbeat());
        writer.writeLong(COLUMN_HEARTBEAT_VERSION, machineInfo.getHeartbeatVersion());
        writer.writeString(COLUMN_HOSTNAME, machineInfo.getHostname());
    }

    @Override
    public void readBinary(BinaryReader reader) throws BinaryObjectException {
        LOGGER.debug("Reader is executing...");
        if (null == machineInfo) {
            machineInfo = new MachineInfo();
        }

        reader.readString(COLUMN_ID);
        machineInfo.setApp(reader.readString(COLUMN_APP));
        machineInfo.setAppType(reader.readInt(COLUMN_APP_TYPE));
        machineInfo.setIp(reader.readString(COLUMN_IP));
        machineInfo.setPort(reader.readInt(COLUMN_PORT));
        machineInfo.setVersion(reader.readString(COLUMN_VERSION));
        machineInfo.setHeartbeatVersion(reader.readLong(COLUMN_LAST_HEARTBEAT));
        machineInfo.setHeartbeatVersion(reader.readLong(COLUMN_HEARTBEAT_VERSION));
        machineInfo.setHostname(reader.readString(COLUMN_HOSTNAME));
    }

    public MachineInfo getMachineInfo() {
        return machineInfo;
    }
}
