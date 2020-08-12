package com.lifeofcoder.autolimiter.dashboard.model;

import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.auth.AuthService;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;

/**
 * IgniteAuth
 *
 * @author xbc
 * @date 2020/7/22
 */
public class IgniteAuth implements BaseIgniteModel, Binarylizable, AuthService.AuthUser {
    //    private static Logger LOGGER = LoggerFactory.getLogger(IgniteMetrics.class);

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";

    private String name;
    private String id;

    public IgniteAuth() {
    }

    public IgniteAuth(String id, String name) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String key() {
        return id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        writer.writeString(COLUMN_ID, id);
        writer.writeString(COLUMN_NAME, name);
    }

    @Override
    public void readBinary(BinaryReader reader) throws BinaryObjectException {
        id = reader.readString(COLUMN_ID);
        name = reader.readString(COLUMN_NAME);
    }

    @Override
    public boolean authTarget(String target, AuthService.PrivilegeType privilegeType) {
        return true;
    }

    @Override
    public boolean isSuperUser() {
        return true;
    }

    @Override
    public String getNickName() {
        return name;
    }

    @Override
    public String getLoginName() {
        return name;
    }
}
