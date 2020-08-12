package com.lifeofcoder.autolimiter.dashboard.model;

import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.datasource.entity.MetricEntity;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;

/**
 * IgniteMetrics
 *
 * @author xbc
 * @date 2020/7/22
 */
public class IgniteMetrics implements BaseIgniteModel, Binarylizable {
    //    private static Logger LOGGER = LoggerFactory.getLogger(IgniteMetrics.class);

    public static final String COLUMN_APP = "app";
    public static final String COLUMN_RESOURCE = "resource";
    public static final String COLUMN_GMT_CREATE = "gmt_create";
    public static final String COLUMN_GMT_MODIFIED = "gmt_modified";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_PASS_QPS = "pass_qps";
    public static final String COLUMN_SUCCESS_QPS = "success_qps";
    public static final String COLUMN_BLOCK_QPS = "block_qps";
    public static final String COLUMN_EXCEPTION_QPS = "exception_qps";
    public static final String COLUMN_RT = "rt";
    public static final String COLUMN_COUNT = "count";

    private MetricEntity metricEntity;

    public IgniteMetrics() {
    }

    public IgniteMetrics(MetricEntity metricEntity) {
        this.metricEntity = metricEntity;
    }

    @Override
    public String key() {
        return metricEntity.getApp() + metricEntity.getResource() + metricEntity.getTimestamp().getTime();
    }

    @Override
    public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        writer.writeString(COLUMN_APP, metricEntity.getApp());
        writer.writeString(COLUMN_RESOURCE, metricEntity.getResource());
        writer.writeLong(COLUMN_GMT_CREATE, date2Long(metricEntity.getGmtCreate()));
        writer.writeLong(COLUMN_GMT_MODIFIED, date2Long(metricEntity.getGmtModified()));
        writer.writeLong(COLUMN_TIMESTAMP, date2Long(metricEntity.getTimestamp()));
        writer.writeLong(COLUMN_PASS_QPS, longValue(metricEntity.getPassQps()));
        writer.writeLong(COLUMN_SUCCESS_QPS, longValue(metricEntity.getSuccessQps()));
        writer.writeLong(COLUMN_BLOCK_QPS, longValue(metricEntity.getBlockQps()));
        writer.writeLong(COLUMN_EXCEPTION_QPS, longValue(metricEntity.getExceptionQps()));
        writer.writeDouble(COLUMN_RT, metricEntity.getRt());
        writer.writeInt(COLUMN_COUNT, metricEntity.getCount());
    }

    @Override
    public void readBinary(BinaryReader reader) throws BinaryObjectException {
        if (null == metricEntity) {
            metricEntity = new MetricEntity();
        }
        metricEntity.setApp(reader.readString(COLUMN_APP));
        metricEntity.setResource(reader.readString(COLUMN_RESOURCE));
        metricEntity.setGmtCreate(long2Date(reader.readLong(COLUMN_GMT_CREATE)));
        metricEntity.setGmtModified(long2Date(reader.readLong(COLUMN_GMT_MODIFIED)));
        metricEntity.setTimestamp(long2Date(reader.readLong(COLUMN_TIMESTAMP)));
        metricEntity.setPassQps(reader.readLong(COLUMN_PASS_QPS));
        metricEntity.setSuccessQps(reader.readLong(COLUMN_SUCCESS_QPS));
        metricEntity.setBlockQps(reader.readLong(COLUMN_BLOCK_QPS));
        metricEntity.setExceptionQps(reader.readLong(COLUMN_EXCEPTION_QPS));
        metricEntity.setRt(reader.readDouble(COLUMN_RT));
        metricEntity.setCount(reader.readInt(COLUMN_COUNT));
    }
}
